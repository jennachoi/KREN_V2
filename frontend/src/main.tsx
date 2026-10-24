import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createBrowserRouter, Navigate, RouterProvider } from 'react-router'
import { Layout } from './components/Layout'
import { ComposePage } from './pages/ComposePage'
import { FriendsPage } from './pages/FriendsPage'
import { HomePage } from './pages/HomePage'
import { LettersPage } from './pages/LettersPage'
import { LoginPage } from './pages/LoginPage'
import { MyProfilePage, UserProfilePage } from './pages/ProfilePage'
import { SignupPage } from './pages/SignupPage'
import './index.css'

const queryClient = new QueryClient({
  defaultOptions: { queries: { refetchOnWindowFocus: false } },
})

const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  { path: '/signup', element: <SignupPage /> },
  {
    element: <Layout />,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/friends', element: <FriendsPage /> },
      { path: '/users/:id', element: <UserProfilePage /> },
      { path: '/profile', element: <MyProfilePage /> },
      { path: '/profile/edit', element: <MyProfilePage edit /> },
      { path: '/compose/:userId', element: <ComposePage /> },
      { path: '/letters', element: <LettersPage key="inbox" box="inbox" /> },
      { path: '/letters/sent', element: <LettersPage key="sent" box="sent" /> },
      { path: '/letters/:id', element: <LettersPage key="inbox" box="inbox" /> },
      { path: '*', element: <Navigate to="/" replace /> },
    ],
  },
])

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </StrictMode>,
)
