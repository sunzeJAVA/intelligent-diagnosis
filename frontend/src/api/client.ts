import axios from 'axios'

const controlApi = axios.create({
  baseURL: '/api/control',
  timeout: 10000
})

const dataApi = axios.create({
  baseURL: '/api/data',
  timeout: 30000
})

export { controlApi, dataApi }
