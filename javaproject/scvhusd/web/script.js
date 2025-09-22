const $ = (s) => document.querySelector(s);
const $$ = (s) => Array.from(document.querySelectorAll(s));

let PRODUCTS = [];
let CART = [];
const TAX_RATE = 8.5;

function money(v) { return `$${(Math.round(v * 100) / 100).toFixed(2)}`; }

async function loadProducts() {
  const res = await fetch('/api/products');
  PRODUCTS = await res.json();
  const grid = $('#products');
  grid.innerHTML = '';
  const query = ($('#search')?.value || '').toLowerCase().trim();
  PRODUCTS.filter(p => !query || p.name.toLowerCase().includes(query) || (p.description||'').toLowerCase().includes(query))
    .forEach((p) => {
    const card = document.createElement('div');
    card.className = 'card';
    card.innerHTML = `
      <div class="title">${p.name}</div>
      <div class="desc">${p.description}</div>
      <div class="price">${money(p.price)}</div>
      <button class="add">Add to Cart</button>
    `;
    card.querySelector('.add').addEventListener('click', () => addToCart(p));
    grid.appendChild(card);
  });
}

function addToCart(prod) {
  const existing = CART.find((i) => i.name === prod.name);
  if (existing) existing.quantity += 1;
  else CART.push({ name: prod.name, price: prod.price, quantity: 1 });
  persistCart();
  renderCart();
  switchTo('cart');
}

function renderCart() {
  const cont = $('#cart-items');
  cont.innerHTML = '';
  if (CART.length === 0) {
    cont.innerHTML = '<p>Your cart is empty.</p>';
  }
  CART.forEach((it, idx) => {
    const row = document.createElement('div');
    row.className = 'cart-row';
    row.innerHTML = `
      <div class="c-name">${it.name}</div>
      <div class="c-qty">
        <button class="dec">-</button>
        <input type="number" min="1" value="${it.quantity}" />
        <button class="inc">+</button>
      </div>
      <div class="c-price">${money(it.price)}</div>
      <div class="c-total">${money(it.price * it.quantity)}</div>
      <button class="remove">✕</button>
    `;
    row.querySelector('.dec').onclick = () => { it.quantity = Math.max(1, it.quantity - 1); persistCart(); renderCart(); };
    row.querySelector('.inc').onclick = () => { it.quantity += 1; persistCart(); renderCart(); };
    row.querySelector('input').onchange = (e) => { it.quantity = Math.max(1, parseInt(e.target.value || '1', 10)); persistCart(); renderCart(); };
    row.querySelector('.remove').onclick = () => { CART.splice(idx, 1); persistCart(); renderCart(); };
    cont.appendChild(row);
  });
  updateTotals();
  updateCartCount();
}

function updateTotals() {
  const subtotal = CART.reduce((s, i) => s + i.price * i.quantity, 0);
  const tax = subtotal * (TAX_RATE / 100);
  const total = subtotal + tax;
  $('#subtotal').textContent = money(subtotal);
  $('#tax').textContent = money(tax);
  $('#total').textContent = money(total);
  return { subtotal, tax, total };
}

function switchTo(view) {
  $$('.view').forEach((v) => v.classList.add('hidden'));
  $(`#${view}`).classList.remove('hidden');
  $$('.tab').forEach((t) => t.classList.remove('active'));
  $(`#tab-${view}`).classList.add('active');
}

function setupTabs() {
  $('#tab-shop').onclick = () => switchTo('shop');
  $('#tab-cart').onclick = () => switchTo('cart');
  $('#tab-transactions').onclick = async () => {
    switchTo('transactions');
    await loadTransactions();
  };
}

function setupPaymentTabs() {
  $('#pay-cash').onclick = () => {
    $('#pay-cash').classList.add('active');
    $('#pay-card').classList.remove('active');
    $('#cash-form').classList.remove('hidden');
    $('#card-form').classList.add('hidden');
  };
  $('#pay-card').onclick = () => {
    $('#pay-card').classList.add('active');
    $('#pay-cash').classList.remove('active');
    $('#card-form').classList.remove('hidden');
    $('#cash-form').classList.add('hidden');
  };
}

async function checkout() {
  const btn = $('#checkout');
  btn.disabled = true;
  btn.textContent = 'Processing...';
  const { total } = updateTotals();
  if (CART.length === 0) {
    showMessage('Add items to cart first.');
    btn.disabled = false; btn.textContent = 'Complete Purchase';
    return;
  }
  const cashMode = $('#pay-cash').classList.contains('active');
  const payload = {
    items: CART.map((i) => ({ name: i.name, price: i.price, quantity: i.quantity })),
    paymentMethod: cashMode ? 'CASH' : 'CARD'
  };
  if (cashMode) {
    payload.amountPaid = parseFloat($('#cash-amount').value || '0');
    if (isNaN(payload.amountPaid) || payload.amountPaid < total) {
      showMessage('Cash amount is insufficient for the total.');
      btn.disabled = false; btn.textContent = 'Complete Purchase';
      return;
    }
  } else {
    payload.cardLast4 = ($('#card-last4').value || '').trim();
    payload.cardHolderName = ($('#card-name').value || '').trim();
    payload.cardExpiry = ($('#card-exp').value || '').trim();
    if (!/^\d{4}$/.test(payload.cardLast4)) {
      showMessage('Enter last 4 digits of the card.');
      btn.disabled = false; btn.textContent = 'Complete Purchase';
      return;
    }
    if (!payload.cardHolderName) {
      showMessage('Enter cardholder name.');
      btn.disabled = false; btn.textContent = 'Complete Purchase';
      return;
    }
    if (!/^\d{2}\/\d{2}$/.test(payload.cardExpiry)) {
      showMessage('Enter expiry as MM/YY.');
      btn.disabled = false; btn.textContent = 'Complete Purchase';
      return;
    }
  }

  const res = await fetch('/api/checkout', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    showMessage(data.error || 'Checkout failed');
    btn.disabled = false; btn.textContent = 'Complete Purchase';
    return;
  }
  showMessage('Purchase successful! Transaction #' + data.transactionId, true);
  CART = [];
  persistCart();
  renderCart();
  btn.disabled = false; btn.textContent = 'Complete Purchase';
}

function showMessage(text, ok = false) {
  const m = $('#message');
  m.textContent = text;
  m.className = 'message ' + (ok ? 'ok' : 'err');
}

async function loadTransactions() {
  const res = await fetch('/api/transactions');
  const list = await res.json();
  const div = $('#tx-list');
  div.innerHTML = '';
  if (!Array.isArray(list) || list.length === 0) {
    div.innerHTML = '<p>No transactions found.</p>';
    return;
  }
  list.forEach((t) => {
    const el = document.createElement('div');
    el.className = 'tx';
    el.innerHTML = `
      <div class="t-head">
        <span>#${t.transactionId}</span>
        <span>${t.date}</span>
        <span>${t.method}</span>
        <span>Total ${money(t.total)}</span>
      </div>
      <div class="t-items">
        ${(t.lineItems || []).map(li => `<div class="t-item"><span>${li.description}</span><span>x${li.quantity}</span><span>${money(li.unitPrice)}</span><span>${money(li.lineTotal)}</span></div>`).join('')}
      </div>
    `;
    div.appendChild(el);
  });
}

function updateCartCount() {
  const count = CART.reduce((s, i) => s + i.quantity, 0);
  const badge = $('#cart-count');
  if (badge) badge.textContent = count;
}

function persistCart() {
  try { localStorage.setItem('cart', JSON.stringify(CART)); } catch {}
}

function restoreCart() {
  try {
    const raw = localStorage.getItem('cart');
    if (raw) {
      const arr = JSON.parse(raw);
      if (Array.isArray(arr)) {
        CART = arr.map(i => ({ name: i.name, price: +i.price, quantity: Math.max(1, parseInt(i.quantity||1, 10)) }));
      }
    }
  } catch {}
}

function init() {
  setupTabs();
  setupPaymentTabs();
  $('#checkout').onclick = checkout;
  $('#search')?.addEventListener('input', () => loadProducts());
  $('#clear-cart')?.addEventListener('click', () => { CART = []; persistCart(); renderCart(); });
  restoreCart();
  loadProducts();
  renderCart();
}

document.addEventListener('DOMContentLoaded', init);
