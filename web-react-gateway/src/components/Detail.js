import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useParams } from 'react-router-dom';

function Detail() {
  const [detailData, setDetailData] = useState(null);
  const { id } = useParams();

  useEffect(() => {
    const fetchDetail = async () => {
      try {
        const response = await axios.get(`/detail/${id}`);
        setDetailData(response.data);
      } catch (error) {
        console.error('Error fetching detail data:', error);
      }
    };
    fetchDetail();
  }, [id]);

  const handleUpdate = async () => {
    try {
      await axios.put(`/update/${id}`, detailData);
      alert('Update successful');
    } catch (error) {
      console.error('Error updating data:', error);
    }
  };

  if (!detailData) return <div>Loading...</div>;

  return (
    <div>
      <h2>Detail for ID: {id}</h2>
      <p>Name: {detailData.name}</p>
      <p>Description: {detailData.description}</p>
      <button onClick={handleUpdate}>Update</button>
    </div>
  );
}

export default Detail;