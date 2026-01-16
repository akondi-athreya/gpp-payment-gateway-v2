import './styles.css';

/**
 * Create a payment modal with iframe
 */
export function createModal({ iframeSrc, onClose }) {
  // Remove existing modal if present
  const existing = document.getElementById('payment-gateway-modal');
  if (existing) {
    existing.remove();
  }

  // Create modal container
  const modal = document.createElement('div');
  modal.id = 'payment-gateway-modal';
  modal.setAttribute('data-test-id', 'payment-modal');
  modal.className = 'payment-gateway-modal';

  // Create overlay background
  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';
  overlay.onclick = (e) => {
    // Close modal when clicking outside
    if (e.target === overlay && onClose) {
      destroyModal();
      onClose();
    }
  };

  // Create modal content container
  const content = document.createElement('div');
  content.className = 'modal-content';

  // Create iframe
  const iframe = document.createElement('iframe');
  iframe.src = iframeSrc;
  iframe.setAttribute('data-test-id', 'payment-iframe');
  iframe.className = 'payment-iframe';
  iframe.allow = 'payment;clipboard-read;clipboard-write';
  iframe.frameBorder = '0';
  content.appendChild(iframe);

  // Create close button
  const closeBtn = document.createElement('button');
  closeBtn.className = 'close-button';
  closeBtn.setAttribute('data-test-id', 'close-modal-button');
  closeBtn.innerHTML = '×';
  closeBtn.type = 'button';
  closeBtn.onclick = () => {
    destroyModal();
    if (onClose) {
      onClose();
    }
  };
  content.appendChild(closeBtn);

  // Assemble modal
  overlay.appendChild(content);
  modal.appendChild(overlay);

  // Add to page
  document.body.appendChild(modal);

  console.log('[Modal] Created and appended to DOM');
  return modal;
}

/**
 * Remove the payment modal from DOM
 */
export function destroyModal() {
  const modal = document.getElementById('payment-gateway-modal');
  if (modal && modal.parentNode) {
    modal.parentNode.removeChild(modal);
    console.log('[Modal] Removed from DOM');
  }
}
