import axios from 'axios';
import type {
  MedDispatchRec,
  WeatherAwareDeliveryResult,
  WeatherBlockedResponse,
  LngLat,
  WeatherAwareDeliveryRequest
} from '../types';

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

export const deliveryApi = {
  /**
   * Calculate weather-aware delivery path
   *
   * Supports two modes:
   * 1. Normal mode: Pass only dispatches (auto service point selection)
   * 2. Testing mode: Pass request object with explicit service point and skip flag
   *
   * Testing mode dramatically speeds up international coordinate testing (30-60s → 2-3s)
   * by bypassing the slow ILP service point endpoint query.
   */
  calcWeatherAwareDeliveryPath: async (
    dispatchesOrRequest: MedDispatchRec[] | WeatherAwareDeliveryRequest
  ): Promise<WeatherAwareDeliveryResult | WeatherBlockedResponse> => {
    try {
      let requestBody: any;

      // Check if input is an array (legacy format) or request object (new format)
      if (Array.isArray(dispatchesOrRequest)) {
        // Legacy format: just dispatches array
        requestBody = dispatchesOrRequest;
        console.log('📍 API: Normal mode - auto service point selection');
      } else {
        // New format: WeatherAwareDeliveryRequest object
        requestBody = dispatchesOrRequest;

        if (dispatchesOrRequest.skipServicePointSelection &&
            dispatchesOrRequest.explicitServicePoint) {
          console.log('🧪 API: Testing mode - using explicit service point');
          console.log('   Service Point:', dispatchesOrRequest.explicitServicePoint);
          console.log('   Skipping ILP service point query');
        } else {
          console.log('📍 API: Normal mode - auto service point selection');
        }
      }

      const response = await api.post<WeatherAwareDeliveryResult | WeatherBlockedResponse>(
        '/calcWeatherAwareDeliveryPath',
        requestBody
      );
      return response.data;
    } catch (error) {
      // Handle 400 Bad Request errors from backend validation
      if (axios.isAxiosError(error) && error.response?.status === 400) {
        const errorData = error.response.data as { error?: string; message?: string };
        throw new Error(errorData.message || 'Invalid dispatch request');
      }
      throw error;
    }
  },
};

export default api;
