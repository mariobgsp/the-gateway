package service

import (
	"context"
	"crypto/rand"
	"encoding/base64"
	"strings"

	"github.com/mariobgsp/the-gateway/gateway-go/internal/store"
)

type StoreItem struct {
	ID        int64  `json:"id"`
	StoreName string `json:"storeName"`
	ClientID  string `json:"clientId"`
	SecretKey string `json:"secretKey"`
}

func toStoreItem(s *store.StoreAccount) StoreItem {
	return StoreItem{ID: s.ID, StoreName: s.StoreName, ClientID: s.ClientID, SecretKey: s.SecretKey}
}

func ListStores(ctx context.Context, db *store.Postgres, username string) ([]StoreItem, error) {
	u, err := db.FindUser(ctx, username)
	if err != nil {
		return nil, &SvcError{HTTPStatus: 404, Code: "01", Message: "user not found!"}
	}
	rows, err := db.UserStores(ctx, u.ID)
	if err != nil {
		return nil, err
	}
	out := make([]StoreItem, 0, len(rows))
	for i := range rows {
		out = append(out, toStoreItem(&rows[i]))
	}
	return out, nil
}

func GetStoreDetail(ctx context.Context, db *store.Postgres, username string, storeID int64) (*StoreItem, error) {
	if storeID == 0 {
		return nil, &SvcError{HTTPStatus: 400, Code: "04", Message: "04:BadRequest:store_id is required"}
	}
	u, err := db.FindUser(ctx, username)
	if err != nil {
		return nil, &SvcError{HTTPStatus: 404, Code: "01", Message: "user not found!"}
	}
	s, err := db.OwnedStore(ctx, u.ID, storeID)
	if err != nil {
		return nil, &SvcError{HTTPStatus: 404, Code: "03", Message: "03:StoreNotFound:store not found!"}
	}
	item := toStoreItem(s)
	return &item, nil
}

func SaveStore(ctx context.Context, db *store.Postgres, username string, storeID *int64, storeName, clientID string) (*StoreItem, error) {
	if strings.TrimSpace(storeName) == "" {
		return nil, &SvcError{HTTPStatus: 400, Code: "04", Message: "04:BadRequest:store name is required"}
	}
	if len(storeName) > 255 {
		return nil, &SvcError{HTTPStatus: 400, Code: "04", Message: "04:BadRequest:store name too long"}
	}
	u, err := db.FindUser(ctx, username)
	if err != nil {
		return nil, &SvcError{HTTPStatus: 404, Code: "01", Message: "user not found!"}
	}
	if storeID != nil {
		s, err := db.OwnedStore(ctx, u.ID, *storeID)
		if err != nil {
			return nil, &SvcError{HTTPStatus: 404, Code: "03", Message: "03:StoreNotFound:store not found!"}
		}
		s.StoreName = storeName
		if strings.TrimSpace(clientID) != "" {
			s.ClientID = clientID
		}
		if err := db.UpdateStore(ctx, s); err != nil {
			return nil, err
		}
		item := toStoreItem(s)
		return &item, nil
	}
	if db.StoreNameExists(ctx, storeName) {
		return nil, &SvcError{HTTPStatus: 400, Code: "04", Message: "04:BadRequest:store name already exists"}
	}
	cid := clientID
	if strings.TrimSpace(cid) == "" {
		tok, err := randomToken(6)
		if err != nil {
			return nil, &SvcError{HTTPStatus: 500, Code: "99", Message: "99:internalServerError:" + err.Error()}
		}
		cid = "client_" + tok
	}
	secret, err := generateSecretKey()
	if err != nil {
		return nil, &SvcError{HTTPStatus: 500, Code: "99", Message: "99:internalServerError:" + err.Error()}
	}
	s, err := db.CreateStore(ctx, u.ID, storeName, cid, secret)
	if err != nil {
		return nil, err
	}
	item := toStoreItem(s)
	return &item, nil
}

func DeleteStore(ctx context.Context, db *store.Postgres, username string, storeID int64) error {
	if storeID == 0 {
		return &SvcError{HTTPStatus: 400, Code: "04", Message: "04:BadRequest:store_id is required"}
	}
	u, err := db.FindUser(ctx, username)
	if err != nil {
		return &SvcError{HTTPStatus: 404, Code: "01", Message: "user not found!"}
	}
	if _, err := db.OwnedStore(ctx, u.ID, storeID); err != nil {
		return &SvcError{HTTPStatus: 404, Code: "03", Message: "03:StoreNotFound:store not found!"}
	}
	return db.DeleteStore(ctx, storeID)
}

func RegenerateSecret(ctx context.Context, db *store.Postgres, username string, storeID int64) (*StoreItem, error) {
	if storeID == 0 {
		return nil, &SvcError{HTTPStatus: 400, Code: "04", Message: "04:BadRequest:store_id is required"}
	}
	u, err := db.FindUser(ctx, username)
	if err != nil {
		return nil, &SvcError{HTTPStatus: 404, Code: "01", Message: "user not found!"}
	}
	s, err := db.OwnedStore(ctx, u.ID, storeID)
	if err != nil {
		return nil, &SvcError{HTTPStatus: 404, Code: "03", Message: "03:StoreNotFound:store not found!"}
	}
	s.SecretKey, err = generateSecretKey()
	if err != nil {
		return nil, &SvcError{HTTPStatus: 500, Code: "99", Message: "99:internalServerError:" + err.Error()}
	}
	if err := db.UpdateStore(ctx, s); err != nil {
		return nil, err
	}
	item := toStoreItem(s)
	return &item, nil
}

func generateSecretKey() (string, error) {
	var b [32]byte
	if _, err := rand.Read(b[:]); err != nil {
		return "", err
	}
	return "gw_" + base64.RawURLEncoding.EncodeToString(b[:]), nil
}

func randomToken(n int) (string, error) {
	b := make([]byte, n)
	if _, err := rand.Read(b); err != nil {
		return "", err
	}
	return base64.RawURLEncoding.EncodeToString(b), nil
}
