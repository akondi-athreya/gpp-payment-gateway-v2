import { createModal, destroyModal } from './modal';

const ORIGIN_REGEX = /^https?:\/\//;

/**
 * PaymentGateway SDK - Embeddable payment modal for merchants
 * 
 * @example
 * const checkout = new PaymentGateway({
 *   key: 'key_test_abc123',
 *   orderId: 'order_xyz',
 *   onSuccess: (response) => console.log('Payment successful'),
 *   onFailure: (error) => console.log('Payment failed'),
 *   onClose: () => console.log('Modal closed')
 * });
 * 
 * checkout.open();
 */
export default class PaymentGateway {
  constructor(options = {}) {
    const { key, orderId, onSuccess, onFailure, onClose } = options;

    // Validate required parameters
    if (!key) {
      throw new Error('Payment Gateway key is required');
    }
    if (!orderId) {
      throw new Error('Order ID is required');
    }

    // Store configuration
    this.apiKey = key;
    this.orderId = orderId;
    this.onSuccess = onSuccess;
    this.onFailure = onFailure;
    this.onClose = onClose;
    this.modalOpen = false;
    this.checkoutBaseUrl = this.getCheckoutBaseUrl();

    // Bind message handler
    this.messageHandler = this.handleMessage.bind(this);
  }

  /**
   * Determine checkout base URL based on environment
   */
  getCheckoutBaseUrl() {
    if (typeof window !== 'undefined') {
      const protocol = window.location.protocol;
      const hostname = window.location.hostname;
      
      // Default to port 3001 for development
      if (hostname === 'localhost' || hostname === '127.0.0.1') {
        return `${protocol}//localhost:3001`;
      }

      // For production, use same protocol and hostname with /checkout path
      return `${protocol}//${hostname}:3001`;
    }
    return 'http://localhost:3001';
  }

  /**
   * Open the payment modal
   */
  open() {
    if (this.modalOpen) {
      console.warn('Modal is already open');
      return;
    }

    try {
      // Create iframe URL with parameters
      const iframeSrc = `${this.checkoutBaseUrl}/iframe.html?order_id=${encodeURIComponent(this.orderId)}&key=${encodeURIComponent(this.apiKey)}`;

      // Create and show modal
      createModal({
        iframeSrc,
        onClose: this.handleClose.bind(this)
      });

      this.modalOpen = true;

      // Set up message listener for iframe communication
      window.addEventListener('message', this.messageHandler);

      console.log('[PaymentGateway] Modal opened for order:', this.orderId);
    } catch (error) {
      console.error('[PaymentGateway] Error opening modal:', error);
      if (this.onFailure) {
        this.onFailure({ message: 'Failed to open payment modal' });
      }
    }
  }

  /**
   * Close the payment modal
   */
  close() {
    if (!this.modalOpen) {
      return;
    }

    this.cleanup();
    this.handleClose();
  }

  /**
   * Handle modal close
   */
  handleClose() {
    console.log('[PaymentGateway] Modal closed');
    if (this.onClose) {
      this.onClose();
    }
  }

  /**
   * Handle messages from iframe
   */
  handleMessage(event) {
    // Basic security: check origin
    if (!event.origin.includes('localhost') && !event.origin.includes(window.location.hostname)) {
      return;
    }

    const { type, data } = event.data || {};

    if (!type) {
      return;
    }

    console.log('[PaymentGateway] Message received:', type);

    switch (type) {
      case 'payment_success':
        if (this.onSuccess) {
          this.onSuccess(data);
        }
        this.cleanup();
        break;

      case 'payment_failed':
        if (this.onFailure) {
          this.onFailure(data);
        }
        break;

      case 'close_modal':
        this.cleanup();
        this.handleClose();
        break;

      default:
        console.warn('[PaymentGateway] Unknown message type:', type);
        break;
    }
  }

  /**
   * Clean up event listeners and modal state
   */
  cleanup() {
    destroyModal();
    window.removeEventListener('message', this.messageHandler);
    this.modalOpen = false;
  }
}
