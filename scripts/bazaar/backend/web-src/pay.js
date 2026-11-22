// Bazaar payments in the browser: the express wallet button (product page,
// cart, checkout) and the pay-later / bank form of an order left payment
// pending. Loaded only on those pages. With Stripe it loads Stripe.js from
// js.stripe.com, which the page's policy allows there and nowhere else; with
// the simulated processor it drives the same server flow with a test button.
// Nothing here decides an amount: the server prices every step.
(function () {
  function form(params) {
    var parts = [];
    for (var k in params) {
      if (Object.prototype.hasOwnProperty.call(params, k) && params[k] != null) {
        parts.push(encodeURIComponent(k) + '=' + encodeURIComponent(params[k]));
      }
    }
    return parts.join('&');
  }

  function post(url, params) {
    return fetch(url, {
      method: 'POST', credentials: 'same-origin',
      headers: {'Content-Type': 'application/x-www-form-urlencoded'},
      body: form(params || {})
    }).then(function (r) {
      return r.json().then(function (body) {
        if (!r.ok) {
          throw new Error(body && body.error ? body.error : 'Refused');
        }
        return body;
      });
    });
  }

  function stripeJs() {
    if (window.Stripe) {
      return Promise.resolve(window.Stripe);
    }
    return new Promise(function (resolve, reject) {
      var s = document.createElement('script');
      s.src = 'https://js.stripe.com/v3/';
      s.onload = function () { resolve(window.Stripe); };
      s.onerror = reject;
      document.head.appendChild(s);
    });
  }

  function showError(box, message) {
    var err = box.querySelector('.err');
    if (err) {
      err.textContent = message;
      err.hidden = false;
    }
  }

  // What the product page adds to the cart first, or nothing elsewhere.
  function item() {
    var v = document.querySelector('form.add input[name="variantId"]');
    if (!v) {
      return {};
    }
    var q = document.getElementById('quantity');
    return {variantId: v.value, quantity: q ? q.value : '1'};
  }

  function sheet(s) {
    var items = [];
    for (var i = 0; i < (s.items || []).length; i++) {
      items.push({label: s.items[i].label, amount: s.items[i].amount});
    }
    var options = [];
    for (var j = 0; j < (s.options || []).length; j++) {
      var o = s.options[j];
      options.push({id: o.id, label: o.label, detail: '', amount: o.amount});
    }
    return {items: items, options: options};
  }

  function express(box) {
    var base = box.getAttribute('data-express');
    var key = box.getAttribute('data-key');
    var failed = box.getAttribute('data-error') || 'The payment did not go through.';
    if (!key) {
      // The simulated processor: one button, the same server flow.
      var button = box.querySelector('button.wallet');
      if (!button) {
        return;
      }
      button.addEventListener('click', function () {
        button.disabled = true;
        post(base + '/start', item()).then(function (s) {
          return post(base + '/' + encodeURIComponent(s.id) + '/simulate', {});
        }).then(function (done) {
          window.location.assign(done.url);
        }).catch(function (e) {
          button.disabled = false;
          showError(box, e.message || failed);
        });
      });
      return;
    }
    stripeJs().then(function (Stripe) {
      var stripe = Stripe(key);
      var amount = parseInt(box.getAttribute('data-amount') || '0', 10);
      var pr = stripe.paymentRequest({
        country: box.getAttribute('data-country') || 'US',
        currency: (box.getAttribute('data-currency') || 'usd').toLowerCase(),
        total: {label: box.getAttribute('data-label') || 'Total', amount: amount,
          pending: amount === 0},
        requestPayerName: true, requestPayerEmail: true, requestPayerPhone: false,
        requestShipping: true
      });
      var started = null;
      var shipping = '';
      pr.canMakePayment().then(function (can) {
        if (!can) {
          return;
        }
        var el = stripe.elements().create('paymentRequestButton', {paymentRequest: pr});
        el.on('click', function () {
          // Opening the sheet starts the checkout; the address event waits for it.
          started = post(base + '/start', item());
        });
        el.mount(box.querySelector('.express-button'));
      });
      function partial(a) {
        return {country: a.country, region: a.region, postal: a.postalCode, city: a.city};
      }
      pr.on('shippingaddresschange', function (ev) {
        started.then(function (s) {
          return post(base + '/' + encodeURIComponent(s.id) + '/quote', partial(ev.shippingAddress));
        }).then(function (q) {
          shipping = q.rateId;
          var view = sheet(q);
          ev.updateWith({status: view.options.length ? 'success' : 'invalid_shipping_address',
            total: {label: box.getAttribute('data-label') || 'Total', amount: q.total},
            displayItems: view.items, shippingOptions: view.options});
        }).catch(function () {
          ev.updateWith({status: 'invalid_shipping_address'});
        });
      });
      pr.on('shippingoptionchange', function (ev) {
        started.then(function (s) {
          var a = ev.shippingAddress || {};
          return post(base + '/' + encodeURIComponent(s.id) + '/quote',
            Object.assign(partial(a), {rateId: ev.shippingOption.id}));
        }).then(function (q) {
          shipping = q.rateId;
          ev.updateWith({status: 'success',
            total: {label: box.getAttribute('data-label') || 'Total', amount: q.total},
            displayItems: sheet(q).items});
        }).catch(function () {
          ev.updateWith({status: 'fail'});
        });
      });
      pr.on('paymentmethod', function (ev) {
        var session;
        started.then(function (s) {
          session = s;
          return stripe.confirmCardPayment(s.clientSecret,
            {payment_method: ev.paymentMethod.id}, {handleActions: false});
        }).then(function (result) {
          if (result.error) {
            ev.complete('fail');
            throw result.error;
          }
          ev.complete('success');
          if (result.paymentIntent && result.paymentIntent.status === 'requires_action') {
            return stripe.confirmCardPayment(session.clientSecret);
          }
          return result;
        }).then(function () {
          var a = ev.shippingAddress || {};
          var lines = a.addressLine || [];
          return post(base + '/' + encodeURIComponent(session.id) + '/confirm', {
            name: a.recipient || ev.payerName || '', line1: lines[0] || '', line2: lines[1] || '',
            city: a.city, region: a.region, postal: a.postalCode, country: a.country,
            phone: a.phone || '', email: ev.payerEmail || '',
            rateId: ev.shippingOption ? ev.shippingOption.id : shipping
          });
        }).then(function (done) {
          window.location.assign(done.url);
        }).catch(function (e) {
          showError(box, (e && e.message) || failed);
        });
      });
    }).catch(function () { /* no wallet: the regular checkout stays */ });
  }

  // An order left payment pending: Stripe's Payment Element for the method
  // the shopper chose; the processor's webhook settles the order.
  function pending(f) {
    var key = f.getAttribute('data-key');
    var secret = f.getAttribute('data-secret');
    stripeJs().then(function (Stripe) {
      var stripe = Stripe(key);
      var elements = stripe.elements({clientSecret: secret});
      elements.create('payment').mount(f.querySelector('.pay-element'));
      f.addEventListener('submit', function (e) {
        e.preventDefault();
        stripe.confirmPayment({elements: elements,
          confirmParams: {return_url: f.getAttribute('data-return')}}).then(function (r) {
          if (r.error) {
            showError(f, r.error.message);
          }
        });
      });
    });
  }

  var boxes = document.querySelectorAll('.express[data-express]');
  for (var i = 0; i < boxes.length; i++) {
    express(boxes[i]);
  }
  var f = document.getElementById('pay-form');
  if (f && f.getAttribute('data-secret')) {
    pending(f);
  }
})();
