import { useState, useEffect } from "react";
import { Button } from "../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../components/ui/card";
import { Input } from "../components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../components/ui/select";
import { Badge } from "../components/ui/badge";
import { toast } from "react-hot-toast";
import api from "../services/api";

export default function ImageTemplateManagement() {
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filterTechnology, setFilterTechnology] = useState("");
  const [filterStatus, setFilterStatus] = useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const [technologies, setTechnologies] = useState([]);
  const [buildStatus, setBuildStatus] = useState({});

  useEffect(() => {
    loadTemplates();
    loadTechnologies();
  }, []);

  const loadTemplates = async () => {
    try {
      setLoading(true);
      const response = await api.getWithToken('/images');
      setTemplates(response);
    } catch (error) {
      console.error("Failed to load templates:", error);
      toast.error("Failed to load image templates");
    } finally {
      setLoading(false);
    }
  };

  const loadTechnologies = async () => {
    try {
      const techs = await api.getWithToken('/images/technologies');
      setTechnologies(techs);
    } catch (error) {
      console.error("Failed to load technologies:", error);
    }
  };

  const handleBuildImage = async (templateId) => {
    try {
      setBuildStatus(prev => ({ ...prev, [templateId]: "Building..." }));
      
      const updatedTemplate = await api.postWithToken(`/images/${templateId}/build`);
      
      // Update the template in the list
      setTemplates(prev => 
        prev.map(template => 
          template.id === templateId ? updatedTemplate : template
        )
      );
      
      setBuildStatus(prev => ({ ...prev, [templateId]: "Built successfully!" }));
      toast.success("Image built successfully!");
      
      // Clear status after 3 seconds
      setTimeout(() => {
        setBuildStatus(prev => {
          const newStatus = { ...prev };
          delete newStatus[templateId];
          return newStatus;
        });
      }, 3000);
      
    } catch (error) {
      console.error("Failed to build image:", error);
      setBuildStatus(prev => ({ ...prev, [templateId]: "Build failed" }));
      toast.error("Failed to build image");
    }
  };

  const handleDeleteTemplate = async (templateId) => {
    if (!window.confirm("Are you sure you want to delete this template?")) {
      return;
    }

    try {
      await api.deleteWithToken(`/images/${templateId}`);
      setTemplates(prev => prev.filter(template => template.id !== templateId));
      toast.success("Template deleted successfully!");
    } catch (error) {
      console.error("Failed to delete template:", error);
      toast.error("Failed to delete template");
    }
  };

  const getStatusBadge = (template) => {
    if (template.isBuilt) {
      return <Badge variant="success" className="bg-green-100 text-green-800">Built</Badge>;
    } else {
      return <Badge variant="secondary" className="bg-yellow-100 text-yellow-800">Not Built</Badge>;
    }
  };

  const getTechnologyBadge = (technology) => {
    const colors = {
      python: "bg-blue-100 text-blue-800",
      nodejs: "bg-green-100 text-green-800",
      java: "bg-orange-100 text-orange-800",
      golang: "bg-cyan-100 text-cyan-800"
    };
    
    return (
      <Badge className={colors[technology.toLowerCase()] || "bg-gray-100 text-gray-800"}>
        {technology.charAt(0).toUpperCase() + technology.slice(1)}
      </Badge>
    );
  };

  const filteredTemplates = templates.filter(template => {
    const matchesTechnology = !filterTechnology || template.technology === filterTechnology;
    const matchesStatus = !filterStatus || 
      (filterStatus === "built" && template.isBuilt) ||
      (filterStatus === "unbuilt" && !template.isBuilt);
    const matchesSearch = !searchTerm || 
      template.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      template.description?.toLowerCase().includes(searchTerm.toLowerCase());

    return matchesTechnology && matchesStatus && matchesSearch;
  });

  if (loading) {
    return (
      <div className="container mx-auto p-6">
        <div className="flex items-center justify-center h-64">
          <div className="text-lg">Loading templates...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold">Image Template Management</h1>
        <Button onClick={() => window.location.href = '/image-builder'}>
          Create New Template
        </Button>
      </div>

      {/* Filters */}
      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Filters</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div>
              <label className="block text-sm font-medium mb-1">Search</label>
              <Input
                placeholder="Search templates..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium mb-1">Technology</label>
              <Select value={filterTechnology} onValueChange={setFilterTechnology}>
                <SelectTrigger>
                  <SelectValue placeholder="All technologies" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="">All technologies</SelectItem>
                  {technologies.map(tech => (
                    <SelectItem key={tech} value={tech}>
                      {tech.charAt(0).toUpperCase() + tech.slice(1)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            
            <div>
              <label className="block text-sm font-medium mb-1">Status</label>
              <Select value={filterStatus} onValueChange={setFilterStatus}>
                <SelectTrigger>
                  <SelectValue placeholder="All statuses" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="">All statuses</SelectItem>
                  <SelectItem value="built">Built</SelectItem>
                  <SelectItem value="unbuilt">Not Built</SelectItem>
                </SelectContent>
              </Select>
            </div>
            
            <div className="flex items-end">
              <Button 
                variant="outline" 
                onClick={() => {
                  setFilterTechnology("");
                  setFilterStatus("");
                  setSearchTerm("");
                }}
              >
                Clear Filters
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Templates Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filteredTemplates.map(template => (
          <Card key={template.id} className="hover:shadow-lg transition-shadow">
            <CardHeader>
              <div className="flex justify-between items-start">
                <div className="flex-1">
                  <CardTitle className="text-lg">{template.name}</CardTitle>
                  <div className="flex gap-2 mt-2">
                    {getTechnologyBadge(template.technology)}
                    {template.version && (
                      <Badge variant="outline">v{template.version}</Badge>
                    )}
                    {getStatusBadge(template)}
                  </div>
                </div>
              </div>
            </CardHeader>
            
            <CardContent>
              {template.description && (
                <p className="text-gray-600 mb-4">{template.description}</p>
              )}
              
              <div className="space-y-2 mb-4">
                {template.courseCode && (
                  <div className="text-sm">
                    <span className="font-medium">Course:</span> {template.courseCode}
                  </div>
                )}
                
                {template.dockerImage && (
                  <div className="text-sm">
                    <span className="font-medium">Image:</span> 
                    <code className="bg-gray-100 px-1 rounded text-xs ml-1">
                      {template.dockerImage}
                    </code>
                  </div>
                )}
                
                {template.preInstalledTools && (
                  <div className="text-sm">
                    <span className="font-medium">Tools:</span> 
                    <div className="flex flex-wrap gap-1 mt-1">
                      {JSON.parse(template.preInstalledTools || "[]").slice(0, 3).map(tool => (
                        <Badge key={tool} variant="secondary" className="text-xs">
                          {tool}
                        </Badge>
                      ))}
                      {JSON.parse(template.preInstalledTools || "[]").length > 3 && (
                        <Badge variant="secondary" className="text-xs">
                          +{JSON.parse(template.preInstalledTools || "[]").length - 3} more
                        </Badge>
                      )}
                    </div>
                  </div>
                )}
              </div>
              
              <div className="flex gap-2">
                {!template.isBuilt && (
                  <Button 
                    size="sm"
                    onClick={() => handleBuildImage(template.id)}
                    disabled={buildStatus[template.id] === "Building..."}
                  >
                    {buildStatus[template.id] === "Building..." ? "Building..." : "Build Image"}
                  </Button>
                )}
                
                <Button 
                  size="sm" 
                  variant="outline"
                  onClick={() => window.location.href = `/image-builder?edit=${template.id}`}
                >
                  Edit
                </Button>
                
                <Button 
                  size="sm" 
                  variant="destructive"
                  onClick={() => handleDeleteTemplate(template.id)}
                >
                  Delete
                </Button>
              </div>
              
              {buildStatus[template.id] && (
                <div className="mt-2 p-2 bg-blue-50 border border-blue-200 rounded text-sm">
                  {buildStatus[template.id]}
                </div>
              )}
            </CardContent>
          </Card>
        ))}
      </div>

      {filteredTemplates.length === 0 && (
        <div className="text-center py-12">
          <div className="text-gray-500 text-lg">
            {templates.length === 0 ? "No templates found" : "No templates match the current filters"}
          </div>
          <Button 
            className="mt-4"
            onClick={() => window.location.href = '/image-builder'}
          >
            Create Your First Template
          </Button>
        </div>
      )}
    </div>
  );
} 