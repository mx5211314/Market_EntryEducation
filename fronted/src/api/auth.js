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