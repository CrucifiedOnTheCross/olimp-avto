window.addEventListener('DOMContentLoaded', () => {

    const API_BASE = window.OLIMP_AVTO_CONFIG?.apiBaseUrl ?? '';

    function setFormMessage(form, message, type = 'success') {
        let messageBox = form.querySelector('.form-message');

        if (!messageBox) {
            messageBox = document.createElement('div');
            messageBox.className = 'form-message';
            form.appendChild(messageBox);
        }

        messageBox.textContent = message;
        messageBox.classList.toggle('error', type === 'error');
    }

    async function parseApiMessage(response) {
        try {
            const payload = await response.json();

            if (payload.fields) {
                return Object.values(payload.fields).join('\n');
            }

            return payload.message || 'Не удалось отправить форму';
        } catch {
            return 'Не удалось отправить форму';
        }
    }

    async function submitJsonForm(form, url, payload) {
        const submitButton = form.querySelector('button[type="submit"], .calc-submit, .review-submit');
        const previousText = submitButton ? submitButton.textContent : '';

        if (submitButton) {
            submitButton.disabled = true;
            submitButton.textContent = 'Отправляем...';
        }

        try {
            const response = await fetch(`${API_BASE}${url}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                throw new Error(await parseApiMessage(response));
            }

            const result = await response.json();
            setFormMessage(form, result.message || 'Форма отправлена');
            form.reset();
        } catch (error) {
            setFormMessage(form, error.message, 'error');
        } finally {
            if (submitButton) {
                submitButton.disabled = false;
                submitButton.textContent = previousText;
            }
        }
    }

    async function submitMultipartForm(form, url, formData) {
        const submitButton = form.querySelector('button[type="submit"], .review-submit');
        const previousText = submitButton ? submitButton.textContent : '';

        if (submitButton) {
            submitButton.disabled = true;
            submitButton.textContent = 'Отправляем...';
        }

        try {
            const response = await fetch(`${API_BASE}${url}`, {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                throw new Error(await parseApiMessage(response));
            }

            const result = await response.json();
            setFormMessage(form, result.message || 'Форма отправлена');
            form.reset();

            const preview = document.getElementById('reviewPreview');
            if (preview) {
                preview.innerHTML = '';
            }
        } catch (error) {
            setFormMessage(form, error.message, 'error');
        } finally {
            if (submitButton) {
                submitButton.disabled = false;
                submitButton.textContent = previousText;
            }
        }
    }

    document.querySelectorAll('.calc-form').forEach(form => {
        form.addEventListener('submit', (event) => {
            event.preventDefault();

            if (!form.reportValidity()) {
                return;
            }

            const formData = new FormData(form);

            submitJsonForm(form, '/api/leads', {
                name: String(formData.get('name') || '').trim(),
                phone: String(formData.get('phone') || '').trim(),
                comment: String(formData.get('comment') || '').trim(),
                policyAccepted: formData.get('policyAccepted') === 'on'
            });
        });
    });

    document.querySelectorAll('.review-form').forEach(form => {
        form.addEventListener('submit', (event) => {
            event.preventDefault();

            if (!form.reportValidity()) {
                return;
            }

            const photos = form.querySelector('input[name="photos"]');

            if (photos && photos.files.length > 5) {
                setFormMessage(form, 'Можно загрузить максимум 5 фотографий', 'error');
                return;
            }

            submitMultipartForm(form, '/api/reviews', new FormData(form));
        });
    });

    function escapeHtml(value) {
        return String(value)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }

    function stars(rating) {
        return '★'.repeat(Number(rating) || 0);
    }

    async function loadApprovedReviews() {
        const river = document.querySelector('.reviews-river');
        if (!river) return;

        try {
            const response = await fetch(`${API_BASE}/api/reviews`);
            if (!response.ok) return;

            const reviews = await response.json();
            if (!Array.isArray(reviews) || reviews.length === 0) return;

            river.innerHTML = reviews.map(review => `
                <article class="review-river-card">
                    <div class="stars">${escapeHtml(stars(review.rating))}</div>
                    <p class="review-text">${escapeHtml(review.text)}</p>
                    <div class="review-bottom">
                        <div>
                            <b>${escapeHtml(review.name)}</b>
                            <span>${escapeHtml(review.carModel)} · ${escapeHtml(review.country)}</span>
                        </div>
                    </div>
                </article>
            `).join('');
        } catch {
            // Static fallback reviews stay visible when API is unavailable.
        }
    }

    loadApprovedReviews();

    const reveals = document.querySelectorAll('.reveal');

    reveals.forEach((el, index) => {
        setTimeout(() => {
            el.classList.add('active');
        }, index * 250);
    });

    let lastScroll = 0;
    const header = document.querySelector('.header');

    if (header) {
        window.addEventListener('scroll', () => {
            const currentScroll = window.scrollY;

            if (currentScroll <= 10) {
                header.classList.remove('hide');
                lastScroll = currentScroll;
                return;
            }

            if (currentScroll > lastScroll && currentScroll > 120) {
                header.classList.add('hide');
            } else {
                header.classList.remove('hide');
            }

            lastScroll = currentScroll;
        });
    }

    const modal = document.getElementById('calcModal');
    const openBtns = document.querySelectorAll('.open-modal');
    const closeBtn = document.getElementById('calcClose');

    if (modal) {
        openBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                modal.classList.add('active');
            });
        });

        if (closeBtn) {
            closeBtn.addEventListener('click', () => {
                modal.classList.remove('active');
            });
        }

        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.classList.remove('active');
            }
        });
    }

    const reviewModal = document.getElementById('reviewModal');
    const reviewClose = document.getElementById('reviewClose');
    const reviewButtons = document.querySelectorAll('.open-review');

    if (reviewModal) {
        reviewButtons.forEach(btn => {
            btn.addEventListener('click', () => {
                reviewModal.classList.add('active');
            });
        });

        if (reviewClose) {
            reviewClose.addEventListener('click', () => {
                reviewModal.classList.remove('active');
            });
        }

        reviewModal.addEventListener('click', (e) => {
            if (e.target === reviewModal) {
                reviewModal.classList.remove('active');
            }
        });
    }

    const reviewPhotoInput = document.getElementById('reviewPhotos');
    const reviewPreview = document.getElementById('reviewPreview');

    if (reviewPhotoInput && reviewPreview) {
        reviewPhotoInput.addEventListener('change', () => {
            reviewPreview.innerHTML = '';

            const files = Array.from(reviewPhotoInput.files);

            if (files.length > 5) {
                alert('Можно загрузить максимум 5 фотографий');
                reviewPhotoInput.value = '';
                return;
            }

            files.forEach(file => {
                const reader = new FileReader();

                reader.onload = (e) => {
                    const img = document.createElement('img');
                    img.src = e.target.result;
                    reviewPreview.appendChild(img);
                };

                reader.readAsDataURL(file);
            });
        });
    }

    const fullReviewModal = document.getElementById('fullReviewModal');
    const fullReviewClose = document.getElementById('fullReviewClose');
    const readReviewButtons = document.querySelectorAll('.read-review');

    const fullReviewImage = document.getElementById('fullReviewImage');
    const fullReviewRating = document.getElementById('fullReviewRating');
    const fullReviewName = document.getElementById('fullReviewName');
    const fullReviewCar = document.getElementById('fullReviewCar');
    const fullReviewText = document.getElementById('fullReviewText');

    if (
        fullReviewModal &&
        fullReviewImage &&
        fullReviewRating &&
        fullReviewName &&
        fullReviewCar &&
        fullReviewText
    ) {
        readReviewButtons.forEach(button => {
            button.addEventListener('click', () => {
                fullReviewImage.src = button.dataset.image || '';
                fullReviewRating.textContent = button.dataset.rating || '';
                fullReviewName.textContent = button.dataset.name || '';
                fullReviewCar.textContent = button.dataset.car || '';
                fullReviewText.textContent = button.dataset.text || '';

                fullReviewModal.classList.add('active');
            });
        });

        if (fullReviewClose) {
            fullReviewClose.addEventListener('click', () => {
                fullReviewModal.classList.remove('active');
            });
        }

        fullReviewModal.addEventListener('click', (e) => {
            if (e.target === fullReviewModal) {
                fullReviewModal.classList.remove('active');
            }
        });
    }

    const priceInput = document.getElementById('carPrice');
    const totalPrice = document.getElementById('totalPrice');
    const carRub = document.getElementById('carRub');
    const customs = document.getElementById('customs');
    const delivery = document.getElementById('delivery');
    const service = document.getElementById('service');

    const countryButtons = document.querySelectorAll('.country');
    const presetButtons = document.querySelectorAll('.calc-presets button');

    let currentCountry = 'korea';

    const deliveryPrices = {
        korea: 187000,
        japan: 220000,
        china: 160000
    };

    const servicePrices = {
        korea: 100000,
        japan: 50000,
        china: 100000
    };

    function formatPrice(num) {
        return Math.round(num).toLocaleString('ru-RU');
    }

    function calculate() {
        if (
            !priceInput ||
            !totalPrice ||
            !carRub ||
            !customs ||
            !delivery ||
            !service
        ) return;

        const carPriceRub = Number(priceInput.value) || 0;

        const customsRub = carPriceRub * 0.38;
        const deliveryRub = deliveryPrices[currentCountry] || 0;
        const serviceRub = servicePrices[currentCountry] || 0;

        const total = carPriceRub + customsRub + deliveryRub + serviceRub;

        totalPrice.textContent = formatPrice(total);
        carRub.textContent = `${formatPrice(carPriceRub)} ₽`;
        customs.textContent = `${formatPrice(customsRub)} ₽`;
        delivery.textContent = `${formatPrice(deliveryRub)} ₽`;
        service.textContent = `${formatPrice(serviceRub)} ₽`;
    }

    countryButtons.forEach(button => {
        button.addEventListener('click', () => {
            countryButtons.forEach(btn => btn.classList.remove('active'));
            button.classList.add('active');

            currentCountry = button.dataset.country;
            calculate();
        });
    });

    presetButtons.forEach(button => {
        button.addEventListener('click', () => {
            if (!priceInput) return;

            priceInput.value = button.dataset.price;
            calculate();
        });
    });

    if (priceInput) {
        priceInput.addEventListener('input', calculate);
        calculate();
    }

    const carModal = document.getElementById('carModal');
    const carModalClose = document.getElementById('carModalClose');
    const catalogMoreButtons = document.querySelectorAll('.catalog-more');

    const carModalImage = document.getElementById('carModalImage');
    const carModalCountry = document.getElementById('carModalCountry');
    const carModalTitle = document.getElementById('carModalTitle');
    const carModalInfo = document.getElementById('carModalInfo');
    const carModalPrice = document.getElementById('carModalPrice');
    const carModalText = document.getElementById('carModalText');

    if (
        carModal &&
        carModalClose &&
        carModalImage &&
        carModalCountry &&
        carModalTitle &&
        carModalInfo &&
        carModalPrice &&
        carModalText
    ) {
        catalogMoreButtons.forEach(button => {
            button.addEventListener('click', () => {
                carModalImage.src = button.dataset.image || '';
                carModalCountry.textContent = button.dataset.country || '';
                carModalTitle.textContent = button.dataset.title || '';
                carModalInfo.textContent = button.dataset.info || '';
                carModalPrice.textContent = button.dataset.price || '';
                carModalText.textContent = button.dataset.text || '';

                carModal.classList.add('active');
            });
        });

        carModalClose.addEventListener('click', () => {
            carModal.classList.remove('active');
        });

        carModal.addEventListener('click', (e) => {
            if (e.target === carModal) {
                carModal.classList.remove('active');
            }
        });
    }

});
