/* particles.js — subtle network backdrop (tuned for a calmer, professional feel) */
particlesJS('particles-js', {
  particles: {
    number: {
      value: 28,
      density: { enable: true, value_area: 900 }
    },
    color: { value: '#e2e8f0' },
    shape: { type: 'circle' },
    opacity: {
      value: 0.35,
      random: true,
      anim: { enable: false }
    },
    size: {
      value: 2,
      random: true,
      anim: { enable: false }
    },
    line_linked: {
      enable: true,
      distance: 140,
      color: '#cbd5e1',
      opacity: 0.22,
      width: 1
    },
    move: {
      enable: true,
      speed: 0.45,
      direction: 'none',
      random: true,
      straight: false,
      out_mode: 'out',
      bounce: false
    }
  },
  interactivity: {
    detect_on: 'canvas',
    events: {
      onhover: { enable: true, mode: 'grab' },
      onclick: { enable: false, mode: 'push' },
      resize: true
    },
    modes: {
      grab: {
        distance: 120,
        line_linked: { opacity: 0.35 }
      },
      push: { particles_nb: 2 }
    }
  },
  retina_detect: true
});
