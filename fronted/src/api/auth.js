import axios from 'axios'

export const login = (data) => {
    return axios.post('/api/auth/login', data, {
        headers: {
            'Content-Type': 'application/json'
        }
    }).then(res => res.data)
}

export const register = (data) => {
    return axios.post('/api/auth/register', data, {
        headers: {
            'Content-Type': 'application/json'
        }
    }).then(res => res.data)
}

// GitHub 授权地址由后端拼，client_id 不下发到前端
export const getGithubLoginUrl = () => {
    return axios.get('/api/oauth/github/login-url').then(res => res.data)
}