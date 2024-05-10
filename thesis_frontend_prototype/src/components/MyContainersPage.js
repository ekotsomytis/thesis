import React from 'react';
import axios from 'axios'; // Import Axios library

const MyContainersPage = () => {
  const handleCreateContainer = () => {
    // Make a POST request to the backend API to create a new container
    axios.post('http://localhost:8180/api/containers/nginx')
      .then(response => {
        console.log('Response:', response.data); // Log the response from the server
        // Handle any further logic based on the response if needed
      })
      .catch(error => {
        console.error('Error creating container:', error); // Log any errors
        // Handle error scenarios if needed
      });
  };

  return (
    <div>
      <h1>My Containers</h1>
      <button onClick={handleCreateContainer}>Create Container</button>
    </div>
  );
}

export default MyContainersPage;
