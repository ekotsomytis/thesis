import React from 'react';
import HomePage from './components/HomePage';
import MyContainersPage from './components/MyContainersPage';
import { BrowserRouter, Routes, Route } from 'react-router-dom';;


function App() {
  return (
    <BrowserRouter>
      <Routes> {/* Wrap your routes in <Routes> */}
        <Route path="/" element={<HomePage />} />
        <Route path="/my-containers" element={<MyContainersPage />} /> {/* Use the element prop to specify the component */}
      </Routes>
    </BrowserRouter>
  );
}

export default App;