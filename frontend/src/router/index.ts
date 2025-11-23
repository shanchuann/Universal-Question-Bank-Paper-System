import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import HelloWorld from '../components/HelloWorld.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HelloWorld,
      props: { msg: 'Welcome to Universal Question Bank' }
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView
    }
  ]
})

export default router
