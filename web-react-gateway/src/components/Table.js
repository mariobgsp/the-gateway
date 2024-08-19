import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { Link } from 'react-router-dom';

function Table() {
  const [tableData, setTableData] = useState([]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const response = await axios.get('/table');
        setTableData(response.data);
      } catch (error) {
        console.error('Error fetching table data:', error);
      }
    };
    fetchData();
  }, []);

  return (
    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>Name</th>
          <th>Action</th>
        </tr>
      </thead>
      <tbody>
        {tableData.map((item) => (
          <tr key={item.id}>
            <td>{item.id}</td>
            <td>{item.name}</td>
            <td>
              <Link to={`/detail/${item.id}`}>View Detail</Link>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export default Table;