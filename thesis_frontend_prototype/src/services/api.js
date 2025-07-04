const API_BASE_URL = 'http://localhost:8080/api';

class ApiService {
  constructor() {
    this.token = localStorage.getItem('authToken');
  }

  setToken(token) {
    this.token = token;
    if (token) {
      localStorage.setItem('authToken', token);
    } else {
      localStorage.removeItem('authToken');
    }
  }

  getAuthHeaders() {
    return {
      'Content-Type': 'application/json',
      ...(this.token && { 'Authorization': `Bearer ${this.token}` })
    };
  }

  async request(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    
    const config = {
      headers: this.getAuthHeaders(),
      ...options
    };

    try {
      const response = await fetch(url, config);
      
      if (!response.ok) {
        if (response.status === 401) {
          this.setToken(null);
          window.location.href = '/';
          throw new Error('Authentication failed');
        }
        
        const errorText = await response.text();
        throw new Error(errorText || `HTTP error! status: ${response.status}`);
      }

      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('application/json')) {
        return await response.json();
      }
      
      return await response.text();
    } catch (error) {
      console.error('API request failed:', error);
      throw error;
    }
  }

  // Authentication endpoints
  async login(username, password) {
    const response = await this.request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    });
    
    if (response.token) {
      this.setToken(response.token);
    }
    
    return response;
  }

  async logout() {
    this.setToken(null);
  }

  // Container Templates endpoints
  async getContainerTemplates() {
    return await this.request('/container-templates');
  }

  async createContainerTemplate(template) {
    return await this.request('/container-templates', {
      method: 'POST',
      body: JSON.stringify(template)
    });
  }

  async updateContainerTemplate(id, template) {
    return await this.request(`/container-templates/${id}`, {
      method: 'PUT',
      body: JSON.stringify(template)
    });
  }

  async deleteContainerTemplate(id) {
    return await this.request(`/container-templates/${id}`, {
      method: 'DELETE'
    });
  }

  async getMyTemplates() {
    return await this.request('/container-templates/my-templates');
  }

  async getSshEnabledTemplates() {
    return await this.request('/container-templates/ssh-enabled');
  }

  async getTemplatesByCategory(category) {
    return await this.request(`/container-templates/category/${category}`);
  }

  async searchTemplates(name) {
    return await this.request(`/container-templates/search?name=${encodeURIComponent(name)}`);
  }

  async getExampleTemplates() {
    return await this.request('/container-templates/examples');
  }

  // Container Instance endpoints
  async getMyContainers() {
    return await this.request('/container-instances/my-containers');
  }

  async createContainer(templateId) {
    return await this.request('/container-instances', {
      method: 'POST',
      body: JSON.stringify({ templateId })
    });
  }

  async startContainer(id) {
    return await this.request(`/container-instances/${id}/start`, {
      method: 'POST'
    });
  }

  async stopContainer(id) {
    return await this.request(`/container-instances/${id}/stop`, {
      method: 'POST'
    });
  }

  async deleteContainer(id) {
    return await this.request(`/container-instances/${id}`, {
      method: 'DELETE'
    });
  }

  async getContainerLogs(id) {
    return await this.request(`/container-instances/${id}/logs`);
  }

  // SSH Connection endpoints
  async createSshConnection(containerInstanceId) {
    return await this.request(`/ssh/connect/${containerInstanceId}`, {
      method: 'POST'
    });
  }

  async getSshConnections() {
    return await this.request('/ssh/connections');
  }

  async getSshConnection(id) {
    return await this.request(`/ssh/connections/${id}`);
  }

  async revokeSshConnection(id) {
    return await this.request(`/ssh/connections/${id}`, {
      method: 'DELETE'
    });
  }

  // Kubernetes Pod Management endpoints  
  async getAllPods(allNamespaces = false) {
    return await this.request(`/kubernetes/pods?allNamespaces=${allNamespaces}`);
  }

  async getPodsInNamespace(namespace) {
    return await this.request(`/kubernetes/namespaces/${namespace}/pods`);
  }

  async getPod(namespace, name) {
    return await this.request(`/kubernetes/namespaces/${namespace}/pods/${name}`);
  }

  async createPod(namespace, podData) {
    const params = new URLSearchParams(podData);
    return await this.request(`/kubernetes/namespaces/${namespace}/pods?${params}`, {
      method: 'POST'
    });
  }

  async deletePod(namespace, name) {
    return await this.request(`/kubernetes/namespaces/${namespace}/pods/${name}`, {
      method: 'DELETE'
    });
  }

  async updatePodResources(namespace, name, resources) {
    return await this.request(`/kubernetes/namespaces/${namespace}/pods/${name}/resources`, {
      method: 'PUT',
      body: JSON.stringify(resources)
    });
  }

  // Kubernetes Deployment Management endpoints
  async getAllDeployments(allNamespaces = false) {
    return await this.request(`/kubernetes/deployments?allNamespaces=${allNamespaces}`);
  }

  async getDeploymentsInNamespace(namespace) {
    return await this.request(`/kubernetes/namespaces/${namespace}/deployments`);
  }

  async getDeployment(namespace, name) {
    return await this.request(`/kubernetes/namespaces/${namespace}/deployments/${name}`);
  }

  async createDeployment(namespace, deploymentData) {
    const params = new URLSearchParams(deploymentData);
    return await this.request(`/kubernetes/namespaces/${namespace}/deployments?${params}`, {
      method: 'POST'
    });
  }

  async deleteDeployment(namespace, name) {
    return await this.request(`/kubernetes/namespaces/${namespace}/deployments/${name}`, {
      method: 'DELETE'
    });
  }

  async scaleDeployment(namespace, name, replicas) {
    return await this.request(`/kubernetes/namespaces/${namespace}/deployments/${name}/scale?replicas=${replicas}`, {
      method: 'PATCH'
    });
  }

  async updateDeploymentImage(namespace, name, image) {
    return await this.request(`/kubernetes/namespaces/${namespace}/deployments/${name}/image?image=${encodeURIComponent(image)}`, {
      method: 'PATCH'
    });
  }

  // Kubernetes Namespace Management endpoints
  async getAllNamespaces() {
    return await this.request('/kubernetes/namespaces');
  }

  async getNamespace(name) {
    return await this.request(`/kubernetes/namespaces/${name}`);
  }

  async createNamespace(name, labels = {}) {
    const params = new URLSearchParams({ name, ...labels });
    return await this.request(`/kubernetes/namespaces?${params}`, {
      method: 'POST'
    });
  }

  async deleteNamespace(name) {
    return await this.request(`/kubernetes/namespaces/${name}`, {
      method: 'DELETE'
    });
  }

  // Enhanced Container Instance Management
  async getAllContainers() {
    return await this.request('/container-instances/all');
  }

  async createContainerFromTemplate(templateId, studentId = null) {
    const body = { templateId };
    if (studentId) body.studentId = studentId;
    
    return await this.request('/container-instances/from-template', {
      method: 'POST',
      body: JSON.stringify(body)
    });
  }

  async getContainerStatus(id) {
    return await this.request(`/container-instances/${id}/status`);
  }

  async restartContainer(id) {
    return await this.request(`/container-instances/${id}/restart`, {
      method: 'POST'
    });
  }

  // User Management (for teachers managing students)
  async getAllStudents() {
    return await this.request('/users/students');
  }

  async getUserContainers(userId) {
    return await this.request(`/users/${userId}/containers`);
  }
}

export default new ApiService();
