package service

import (
	"context"
	"crypto/rand"
	"encoding/base64"
	"strings"

	"github.com/mariobgsp/the-gateway/gateway-go/internal/api"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/store"
)

// mustUser resolves the authenticated caller.
func mustUser(ctx context.Context, db *store.Postgres, username string) (*store.User, error) {
	u, err := db.FindUser(ctx, username)
	if err != nil {
		return nil, api.UserNotFound()
	}
	return u, nil
}

// mustOwnedStore resolves a store belonging to the caller, guarding the id
// shape first and the ownership second.
func mustOwnedStore(ctx context.Context, db *store.Postgres, username string, storeID int64) (*store.StoreAccount, error) {
	if storeID == 0 {
		return nil, api.BadRequest("store_id is required")
	}
	u, err := mustUser(ctx, db, username)
	if err != nil {
		return nil, err
	}
	s, err := db.OwnedStore(ctx, u.ID, storeID)
	if err != nil {
		return nil, api.StoreNotFound()
	}
	return s, nil
}

func ListStores(ctx context.Context, db *store.Postgres, username string) ([]store.StoreAccount, error) {
	u, err := mustUser(ctx, db, username)
	if err != nil {
		return nil, err
	}
	return db.UserStores(ctx, u.ID)
}

func GetStoreDetail(ctx context.Context, db *store.Postgres, username string, storeID int64) (*store.StoreAccount, error) {
	return mustOwnedStore(ctx, db, username, storeID)
}

// SaveStore renames an existing store, or creates one with a generated client
// id and secret key.
func SaveStore(ctx context.Context, db *store.Postgres, username string, storeID *int64, storeName, clientID string) (*store.StoreAccount, error) {
	if strings.TrimSpace(storeName) == "" {
		return nil, api.BadRequest("store name is required")
	}
	if len(storeName) > 255 {
		return nil, api.BadRequest("store name too long")
	}
	u, err := mustUser(ctx, db, username)
	if err != nil {
		return nil, err
	}
	if storeID != nil {
		s, err := db.OwnedStore(ctx, u.ID, *storeID)
		if err != nil {
			return nil, api.StoreNotFound()
		}
		s.StoreName = storeName
		if strings.TrimSpace(clientID) != "" {
			s.ClientID = clientID
		}
		return s, db.UpdateStore(ctx, s)
	}
	if db.StoreNameExists(ctx, storeName) {
		return nil, api.BadRequest("store name already exists")
	}
	if strings.TrimSpace(clientID) == "" {
		if clientID, err = randomToken("client_", 6); err != nil {
			return nil, api.Internal(err)
		}
	}
	secret, err := randomToken("gw_", 32)
	if err != nil {
		return nil, api.Internal(err)
	}
	return db.CreateStore(ctx, u.ID, storeName, clientID, secret)
}

func DeleteStore(ctx context.Context, db *store.Postgres, username string, storeID int64) error {
	if _, err := mustOwnedStore(ctx, db, username, storeID); err != nil {
		return err
	}
	return db.DeleteStore(ctx, storeID)
}

func RegenerateSecret(ctx context.Context, db *store.Postgres, username string, storeID int64) (*store.StoreAccount, error) {
	s, err := mustOwnedStore(ctx, db, username, storeID)
	if err != nil {
		return nil, err
	}
	if s.SecretKey, err = randomToken("gw_", 32); err != nil {
		return nil, api.Internal(err)
	}
	if err := db.UpdateStore(ctx, s); err != nil {
		return nil, err
	}
	return s, nil
}

// randomToken returns prefix followed by n random bytes, URL-base64 encoded.
func randomToken(prefix string, n int) (string, error) {
	b := make([]byte, n)
	if _, err := rand.Read(b); err != nil {
		return "", err
	}
	return prefix + base64.RawURLEncoding.EncodeToString(b), nil
}
