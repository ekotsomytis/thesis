import { useState, useEffect } from "react";
import { Button } from "../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../components/ui/card";
import { Input } from "../components/ui/input";
import { toast } from "react-hot-toast";
import api from "../services/api";
import { useAuth } from "../contexts/AuthContext";

export default function StudentContainers() {
  const { user, isTeacher } = useAuth();
  const [containers, setContainers] = useState([]);
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState("");
  const [logs, setLogs] = useState({});
  const [showLogsFor, setShowLogsFor] = useState(null);
  const [sshConnections, setSshConnections] = useState([]);
  const [showSshModal, setShowSshModal] = useState(false);
  const [selectedContainer, setSelectedContainer] = useState(null);
  const [sshInfo, setSshInfo] = useState(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      
      // Ensure API has the current token
      if (user && user.token) {
        api.setToken(user.token);
      }
      
      const [containersResponse, templatesResponse] = await Promise.all([
        isTeacher() ? api.getAllContainers() : api.getMyContainers(),
        api.getImageTemplates() // Use ImageTemplates instead of ContainerTemplates
      ]);
      
      setContainers(containersResponse || []);
      setTemplates(templatesResponse || []);
      
      console.log('Loaded containers:', containersResponse);
      console.log('Loaded templates:', templatesResponse);
      
      if (!isTeacher()) {
        // Load SSH connections for students
        try {
          const sshResponse = await api.getSshConnections();
          setSshConnections(sshResponse || []);
          console.log('Loaded SSH connections:', sshResponse);
        } catch (error) {
          console.error('Failed to load SSH connections:', error);
          // Don't fail the whole load if SSH connections fail
        }
      }
    } catch (error) {
      console.error("Failed to load data:", error);
      toast.error("Failed to load container data");
    } finally {
      setLoading(false);
    }
  };

  const handleCreateContainer = async () => {
    if (!selectedTemplate) {
      toast.error("Please select a template");
      return;
    }

    try {
      await api.createContainerFromTemplate(selectedTemplate);
      toast.success("Container created successfully");
      setShowCreateModal(false);
      setSelectedTemplate("");
      loadData();
    } catch (error) {
      console.error("Failed to create container:", error);
      toast.error("Failed to create container");
    }
  };

  const handleStartContainer = async (containerId) => {
    try {
      await api.startContainer(containerId);
      toast.success("Container started");
      loadData();
    } catch (error) {
      console.error("Failed to start container:", error);
      toast.error("Failed to start container");
    }
  };

  const handleStopContainer = async (containerId) => {
    try {
      await api.stopContainer(containerId);
      toast.success("Container stopped");
      loadData();
    } catch (error) {
      console.error("Failed to stop container:", error);
      toast.error("Failed to stop container");
    }
  };

  const handleDeleteContainer = async (containerId) => {
    if (!window.confirm("Are you sure you want to delete this container? This action cannot be undone.")) {
      return;
    }

    try {
      await api.deleteContainer(containerId);
      toast.success("Container deleted");
      loadData();
    } catch (error) {
      console.error("Failed to delete container:", error);
      toast.error("Failed to delete container");
    }
  };

  const handleGetLogs = async (containerId) => {
    try {
      const containerLogs = await api.getContainerLogs(containerId);
      setLogs(prev => ({
        ...prev,
        [containerId]: containerLogs
      }));
      setShowLogsFor(containerId);
    } catch (error) {
      console.error("Failed to get logs:", error);
      toast.error("Failed to retrieve container logs");
    }
  };

  const handleCreateSshConnection = async (containerId) => {
    try {
      const connection = await api.createSshConnection(containerId);
      toast.success("SSH connection created successfully");
      setSshConnections(prev => [...prev, connection]);
    } catch (error) {
      console.error("Failed to create SSH connection:", error);
      toast.error("Failed to create SSH connection");
    }
  };

  const handleRevokeSshConnection = async (connectionId) => {
    try {
      await api.revokeSshConnection(connectionId);
      toast.success("SSH connection revoked");
      setSshConnections(prev => prev.filter(conn => conn.id !== connectionId));
    } catch (error) {
      console.error("Failed to revoke SSH connection:", error);
      toast.error("Failed to revoke SSH connection");
    }
  };

  const handleShowSshInfo = async (container) => {
    try {
      // Ensure API has the current token
      if (user && user.token) {
        api.setToken(user.token);
      }
      
      const info = await api.getContainerSshInfo(container.id);
      setSelectedContainer(container);
      setSshInfo(info);
      setShowSshModal(true);
    } catch (error) {
      console.error('Failed to get SSH info:', error);
      toast.error('Failed to get SSH information: ' + error.message);
    }
  };

  const getStatusColor = (status) => {
    const colors = {
      'Running': 'text-green-600',
      'Pending': 'text-yellow-600',
      'Stopped': 'text-red-600',
      'Creating': 'text-blue-600',
      'Error': 'text-red-800',
      'Unknown': 'text-gray-600'
    };
    return colors[status] || colors.Unknown;
  };

  const getStatusIcon = (status) => {
    const icons = {
      'Running': '🟢',
      'Pending': '🟡',
      'Stopped': '🔴',
      'Creating': '🔵',
      'Error': '❌',
      'Unknown': '❓'
    };
    return icons[status] || icons.Unknown;
  };

  if (loading) {
    return (
      <div className="p-6 flex justify-center">
        <div className="text-lg">Loading containers...</div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold">
          {isTeacher() ? "All Student Containers" : "My Containers"}
        </h1>
        {!isTeacher() && (
          <Button 
            onClick={() => setShowCreateModal(true)}
            className="bg-blue-600 hover:bg-blue-700"
          >
            Create New Container
          </Button>
        )}
      </div>

      {/* Create Container Modal */}
      {showCreateModal && (
        <Card>
          <CardHeader>
            <CardTitle>Create New Container</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-2">Select Template</label>
                <select
                  value={selectedTemplate}
                  onChange={(e) => setSelectedTemplate(e.target.value)}
                  className="w-full border rounded-md px-3 py-2"
                >
                  <option value="">Choose a template...</option>
                  {templates.map((template) => (
                    <option key={template.id} value={template.id}>
                      {template.name} ({template.dockerImage})
                    </option>
                  ))}
                </select>
              </div>
              
              <div className="flex space-x-2">
                <Button onClick={handleCreateContainer} className="bg-green-600 hover:bg-green-700">
                  Create Container
                </Button>
                <Button 
                  onClick={() => setShowCreateModal(false)}
                  className="bg-gray-500 hover:bg-gray-600"
                >
                  Cancel
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Containers List */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {containers.map((container) => (
          <Card key={container.id}>
            <CardHeader>
              <div className="flex justify-between items-start">
                <div>
                  <CardTitle className="text-lg">{container.name}</CardTitle>
                  {isTeacher() && container.owner && (
                    <p className="text-sm text-gray-600">Owner: {container.owner.username}</p>
                  )}
                </div>
                <div className={`flex items-center space-x-1 ${getStatusColor(container.status)}`}>
                  <span>{getStatusIcon(container.status)}</span>
                  <span className="font-medium">{container.status}</span>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="text-sm">
                  <strong>Pod Name:</strong> {container.kubernetesPodName}
                </div>
                
                {/* Container Actions */}
                <div className="flex flex-wrap gap-2">
                  {container.status === 'Stopped' && (
                    <Button
                      size="sm"
                      onClick={() => handleStartContainer(container.id)}
                      className="bg-green-500 hover:bg-green-600"
                    >
                      Start
                    </Button>
                  )}
                  
                  {container.status === 'Running' && (
                    <Button
                      size="sm"
                      onClick={() => handleStopContainer(container.id)}
                      className="bg-yellow-500 hover:bg-yellow-600"
                    >
                      Stop
                    </Button>
                  )}
                  
                  <Button
                    size="sm"
                    onClick={() => handleGetLogs(container.id)}
                    className="bg-blue-500 hover:bg-blue-600"
                  >
                    View Logs
                  </Button>
                  
                  {!isTeacher() && container.status === 'Running' && (
                    <Button
                      size="sm"
                      onClick={() => handleShowSshInfo(container)}
                      className="bg-green-500 hover:bg-green-600"
                    >
                      SSH Info
                    </Button>
                  )}
                  
                  <Button
                    size="sm"
                    onClick={() => handleDeleteContainer(container.id)}
                    className="bg-red-500 hover:bg-red-600"
                  >
                    Delete
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {containers.length === 0 && (
        <Card>
          <CardContent className="text-center py-8">
            <div className="text-gray-500">
              {isTeacher() 
                ? "No student containers found."
                : "No containers found. Create your first container to get started."
              }
            </div>
          </CardContent>
        </Card>
      )}

      {/* SSH Connections Section for Students */}
      {!isTeacher() && sshConnections.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Active SSH Connections</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {sshConnections.map((connection) => (
                <div key={connection.id} className="flex justify-between items-center border-b pb-2">
                  <div>
                    <div className="font-medium">{connection.containerName}</div>
                    <div className="text-sm text-gray-600">
                      Host: {connection.sshHost}:{connection.sshPort}
                    </div>
                    <div className="text-sm text-gray-600">
                      Username: {connection.username}
                    </div>
                  </div>
                  <Button
                    size="sm"
                    onClick={() => handleRevokeSshConnection(connection.id)}
                    className="bg-red-500 hover:bg-red-600"
                  >
                    Revoke
                  </Button>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Logs Modal */}
      {showLogsFor && (
        <Card className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-lg w-full max-w-4xl max-h-[80vh] overflow-hidden">
            <CardHeader>
              <div className="flex justify-between items-center">
                <CardTitle>Container Logs</CardTitle>
                <Button
                  size="sm"
                  onClick={() => setShowLogsFor(null)}
                  className="bg-gray-500 hover:bg-gray-600"
                >
                  Close
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              <pre className="bg-gray-900 text-green-400 p-4 rounded overflow-auto max-h-96 text-sm">
                {logs[showLogsFor] || "No logs available"}
              </pre>
            </CardContent>
          </div>
        </Card>
      )}

      {/* SSH Info Modal */}
      {showSshModal && selectedContainer && sshInfo && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <h2 className="text-xl font-bold mb-4">SSH Connection Info</h2>
            
            <div className="space-y-4">
              <div className="bg-gray-50 p-4 rounded-md">
                <h3 className="font-medium mb-2">Container: {selectedContainer.name}</h3>
                <p className="text-sm text-gray-600 mb-2">Image: {sshInfo.dockerImage}</p>
                
                {sshInfo.ready ? (
                  <div className="space-y-2">
                    <div className="grid grid-cols-2 gap-2 text-sm">
                      <div><strong>Host:</strong> {sshInfo.host}</div>
                      <div><strong>Port:</strong> {sshInfo.port}</div>
                      <div><strong>Username:</strong> {sshInfo.username}</div>
                      <div><strong>Password:</strong> {sshInfo.password}</div>
                    </div>
                    
                    <div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded">
                      <p className="text-sm text-blue-800">
                        <strong>SSH Command:</strong>
                      </p>
                      <code className="text-xs bg-blue-100 p-1 rounded block mt-1">
                        ssh -p {sshInfo.port} {sshInfo.username}@{sshInfo.host}
                      </code>
                    </div>
                    
                    <div className="mt-2 p-2 bg-yellow-50 border border-yellow-200 rounded">
                      <p className="text-xs text-yellow-800">
                        {sshInfo.instructions}
                      </p>
                    </div>
                  </div>
                ) : (
                  <div className="p-3 bg-orange-50 border border-orange-200 rounded">
                    <p className="text-sm text-orange-800">
                      {sshInfo.message}
                    </p>
                  </div>
                )}
              </div>
            </div>

            <div className="flex gap-2 mt-6">
              <Button
                onClick={() => setShowSshModal(false)}
                className="bg-gray-600 hover:bg-gray-700 text-white flex-1"
              >
                Close
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
