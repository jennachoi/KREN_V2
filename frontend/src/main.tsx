import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createBrowserRouter, Navigate, RouterProvider } from 'react-router'
import { Layout } from './components/Layout'
import { LoginPage, SignupPage } from './pages/AuthPages'
import { ComposePage, LetterPage, MailboxPage } from './pages/LetterPages'
import { PenpalsPage } from './pages/PenpalsPage'
import { ProfilePage } from './pages/ProfilePage'
import { UserPage } from './pages/UserPage'
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
      { path: '/', element: <Navigate to="/penpals" replace /> },
      { path: '/penpals', element: <PenpalsPage /> },
      { path: '/users/:id', element: <UserPage /> },
      { path: '/compose/:userId', element: <ComposePage /> },
      { path: '/inbox', element: <MailboxPage key="inbox" box="inbox" /> },
      { path: '/sent', element: <MailboxPage key="sent" box="sent" /> },
      { path: '/letters/:id', element: <LetterPage /> },
      { path: '/profile', element: <ProfilePage /> },
      { path: '*', element: <Navigate to="/penpals" replace /> },
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
