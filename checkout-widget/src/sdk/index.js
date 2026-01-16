import PaymentGateway from './PaymentGateway';

// Expose PaymentGateway globally
if (typeof window !== 'undefined') {
  window.PaymentGateway = PaymentGateway;
}

export default PaymentGateway;
