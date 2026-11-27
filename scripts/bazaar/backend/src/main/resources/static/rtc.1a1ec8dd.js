// One side of a Bazaar chat call, shared by the shopper's floating chat
// (chat.js) and the store's inbox (app.js). The signalling is the one the
// Parley sample's calls use -- RING, OFFER, ANSWER, ICE, END over the chat
// socket -- and this file only knows the browser half: RTCPeerConnection,
// the microphone and camera, and the candidates that arrive before the
// description they belong to. ICE servers come from the server (STUN from a
// property, TURN credentials minted per connection), never from here.
(function () {
  'use strict';
  function BazaarCall(opts) {
    this.opts = opts;
    this.pending = [];
    this.stream = null;
    this.muted = false;
    var self = this;
    var pc = new RTCPeerConnection({ iceServers: opts.ice || [] });
    this.pc = pc;
    pc.onicecandidate = function (e) {
      if (e.candidate) { opts.signal('ICE', { candidate: e.candidate.toJSON() }); }
    };
    pc.ontrack = function (e) {
      if (opts.remote && e.streams && e.streams[0]) { opts.remote.srcObject = e.streams[0]; }
    };
    pc.onconnectionstatechange = function () {
      if (opts.state) { opts.state(pc.connectionState); }
    };
  }
  BazaarCall.supported = function () {
    return !!(window.RTCPeerConnection && navigator.mediaDevices
        && navigator.mediaDevices.getUserMedia);
  };
  BazaarCall.prototype.media = function (video) {
    var self = this;
    return navigator.mediaDevices.getUserMedia({ audio: true, video: !!video })
      .then(function (stream) {
        self.stream = stream;
        stream.getTracks().forEach(function (t) { self.pc.addTrack(t, stream); });
        if (self.opts.local) { self.opts.local.srcObject = video ? stream : null; }
        return stream;
      });
  };
  // The caller: media, then an offer.
  BazaarCall.prototype.offer = function (video) {
    var self = this;
    return this.media(video).then(function () {
      return self.pc.createOffer();
    }).then(function (d) {
      return self.pc.setLocalDescription(d);
    }).then(function () {
      self.opts.signal('OFFER', { sdp: self.pc.localDescription.sdp, video: !!video });
    });
  };
  // The answerer: the caller's offer, media, then an answer.
  BazaarCall.prototype.answer = function (sdp, video) {
    var self = this;
    return this.pc.setRemoteDescription({ type: 'offer', sdp: sdp }).then(function () {
      self.flush();
      return self.media(video);
    }).then(function () {
      return self.pc.createAnswer();
    }).then(function (d) {
      return self.pc.setLocalDescription(d);
    }).then(function () {
      self.opts.signal('ANSWER', { sdp: self.pc.localDescription.sdp });
    });
  };
  // The caller hears back.
  BazaarCall.prototype.accepted = function (sdp) {
    var self = this;
    return this.pc.setRemoteDescription({ type: 'answer', sdp: sdp }).then(function () {
      self.flush();
    });
  };
  // A remote candidate; held until the remote description is set.
  BazaarCall.prototype.candidate = function (c) {
    if (!c) { return; }
    if (this.pc.remoteDescription) {
      this.pc.addIceCandidate(c).catch(function () { /* a stale candidate */ });
    } else {
      this.pending.push(c);
    }
  };
  BazaarCall.prototype.flush = function () {
    var list = this.pending;
    this.pending = [];
    for (var i = 0; i < list.length; i++) { this.candidate(list[i]); }
  };
  BazaarCall.prototype.mute = function () {
    this.muted = !this.muted;
    var on = !this.muted;
    if (this.stream) {
      this.stream.getAudioTracks().forEach(function (t) { t.enabled = on; });
    }
    return this.muted;
  };
  BazaarCall.prototype.close = function () {
    if (this.stream) { this.stream.getTracks().forEach(function (t) { t.stop(); }); }
    this.stream = null;
    try { this.pc.close(); } catch (e) { /* already closed */ }
    if (this.opts.local) { this.opts.local.srcObject = null; }
    if (this.opts.remote) { this.opts.remote.srcObject = null; }
  };
  window.BazaarCall = BazaarCall;
})();
