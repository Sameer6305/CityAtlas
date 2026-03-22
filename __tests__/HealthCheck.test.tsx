import { render, screen } from '@testing-library/react';
import NotFound from '@/app/not-found';

describe('HealthCheck component baseline', () => {
  test('renders without crashing and shows expected text', () => {
    // FIXED: Basic component mount test for frontend smoke coverage baseline.
    render(<NotFound />);
    expect(screen.getByText(/Page not found/i)).toBeInTheDocument();
  });
});
