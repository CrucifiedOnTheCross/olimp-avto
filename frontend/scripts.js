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
                document.querySelectorAll('.car-modal.active, .review-modal.active, .full-review-modal.active, .auction-lead-modal.active')
                    .forEach(activeModal => activeModal.classList.remove('active'));
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

    const carModalImage = document.getElementById('carModalImage');
    const carModalCountry = document.getElementById('carModalCountry');
    const carModalTitle = document.getElementById('carModalTitle');
    const carModalInfo = document.getElementById('carModalInfo');
    const carModalPrice = document.getElementById('carModalPrice');
    const carModalText = document.getElementById('carModalText');

    const catalogModeButtons = document.querySelectorAll('[data-catalog-mode]');
    const catalogModePanels = document.querySelectorAll('[data-catalog-panel]');
    const catalogFilterList = document.getElementById('catalogFilterList');
    const catalogGrid = document.getElementById('catalogGrid');
    const catalogEmpty = document.getElementById('catalogEmpty');
    const catalogLoadMore = document.getElementById('catalogLoadMore');
    const rateCaptchaModal = document.getElementById('rateCaptchaModal');
    const rateCaptchaClose = document.getElementById('rateCaptchaClose');
    const rateCaptchaQuestion = document.getElementById('rateCaptchaQuestion');
    const rateCaptchaAnswer = document.getElementById('rateCaptchaAnswer');
    const rateCaptchaId = document.getElementById('rateCaptchaId');
    const rateCaptchaVerify = document.getElementById('rateCaptchaVerify');
    const rateCaptchaError = document.getElementById('rateCaptchaError');
    let catalogCars = [];
    let catalogFilter = { type: 'all', value: 'all' };
    let catalogLoading = false;
    let rateCaptchaRetry = null;
    const catalogBatchSize = 4;
    const catalogMaxCars = 96;
    const popularAuctionSources = [
        { value: 'japan', country: 'Япония', currency: '¥', offset: 0, done: false },
        { value: 'korea', country: 'Корея', currency: '₩', offset: 0, done: false },
        { value: 'china', country: 'Китай', currency: '¥', offset: 0, done: false }
    ];

    function setCatalogMode(mode) {
        catalogModeButtons.forEach(button => {
            button.classList.toggle('active', button.dataset.catalogMode === mode);
        });
        catalogModePanels.forEach(panel => {
            panel.classList.toggle('active', panel.dataset.catalogPanel === mode);
        });

        if (mode === 'auction') {
            const url = new URL(window.location.href);
            url.searchParams.set('mode', 'auction');
            window.history.replaceState({}, '', url);
        } else if (window.location.search.includes('mode=')) {
            const url = new URL(window.location.href);
            url.searchParams.delete('mode');
            window.history.replaceState({}, '', url);
        }
    }

    if (catalogModeButtons.length && catalogModePanels.length) {
        catalogModeButtons.forEach(button => {
            button.addEventListener('click', () => setCatalogMode(button.dataset.catalogMode || 'popular'));
        });

        const requestedMode = new URLSearchParams(window.location.search).get('mode');
        setCatalogMode(requestedMode === 'auction' ? 'auction' : 'popular');
    }

    function carManufacturer(title) {
        return String(title || '').trim().split(/\s+/)[0] || 'Другое';
    }

    function renderCatalogFilters(filters) {
        if (!catalogFilterList) return;

        const countries = Array.isArray(filters?.countries) ? filters.countries : [];
        const manufacturers = Array.isArray(filters?.manufacturers) ? filters.manufacturers : [];
        catalogFilterList.innerHTML = `
            <button class="catalog-category ${catalogFilter.type === 'all' ? 'active' : ''}"
                    data-filter-type="all" data-filter-value="all">Все</button>
            ${countries.map(country => `
                <button class="catalog-category ${catalogFilter.type === 'country' && catalogFilter.value === country ? 'active' : ''}"
                        data-filter-type="country" data-filter-value="${escapeHtml(country)}">
                    ${escapeHtml(country)}
                </button>
            `).join('')}
            ${manufacturers.length ? `<div class="catalog-filter-subtitle">Марки автомобилей</div>` : ''}
            ${manufacturers.map(manufacturer => `
                <button class="catalog-category ${catalogFilter.type === 'manufacturer' && catalogFilter.value === manufacturer ? 'active' : ''}"
                        data-filter-type="manufacturer" data-filter-value="${escapeHtml(manufacturer)}">
                    ${escapeHtml(manufacturer)}
                </button>
            `).join('')}
        `;

        catalogFilterList.querySelectorAll('.catalog-category').forEach(button => {
            button.addEventListener('click', () => {
                catalogFilterList.querySelectorAll('.catalog-category').forEach(item => item.classList.remove('active'));
                button.classList.add('active');
                catalogFilter = {
                    type: button.dataset.filterType || 'all',
                    value: button.dataset.filterValue || 'all'
                };
                renderCatalogCars(catalogCars);
            });
        });
    }

    function renderCatalogCars(cars) {
        if (!catalogGrid) return;

        const filtered = cars.filter(car => {
            if (catalogFilter.type === 'country') {
                return car.country === catalogFilter.value;
            }
            if (catalogFilter.type === 'manufacturer') {
                return (car.manufacturer || carManufacturer(car.title)) === catalogFilter.value;
            }
            return true;
        });

        catalogGrid.innerHTML = filtered.map(car => {
            const image = car.imageUrl || '';
            const info = [car.year, car.engine, car.lot ? `лот ${car.lot}` : ''].filter(Boolean).join(' · ');
            return `
                <article class="catalog-car" data-country="${escapeHtml(car.country || '')}">
                    <div class="catalog-car-img">
                        ${image
                            ? `<img src="${escapeHtml(image)}" alt="${escapeHtml(car.title || 'Автомобиль')}">`
                            : '<div class="auction-card-placeholder">Фото пока не передано</div>'}
                        <span>${escapeHtml(car.country || 'Авто')}</span>
                    </div>
                    <div class="catalog-car-body">
                        <h3>${escapeHtml(car.title || 'Автомобиль')}</h3>
                        <p>${escapeHtml(info || car.description || 'Подробности уточним при расчёте')}</p>
                        <strong>${escapeHtml(car.priceLabel || 'Расчёт по запросу')}</strong>
                        <button
                            class="catalog-more"
                            data-lot-id="${escapeHtml(car.id || '')}"
                            data-lot-source="${escapeHtml(car.source || 'japan')}"
                            data-title="${escapeHtml(car.title || 'Автомобиль')}"
                            data-country="${escapeHtml(car.country || '')}"
                            data-price="${escapeHtml(car.priceLabel || 'Расчёт по запросу')}"
                            data-image="${escapeHtml(image)}"
                            data-info="${escapeHtml(info)}"
                            data-text="${escapeHtml(car.description || 'Оставьте заявку, и менеджер подготовит расчёт под ключ.')}">
                            Посмотреть лот
                        </button>
                    </div>
                </article>
            `;
        }).join('');

        if (catalogEmpty) {
            catalogEmpty.hidden = filtered.length > 0;
        }
    }

    function catalogFilters() {
        return {
            countries: [...new Set(catalogCars.map(car => car.country))],
            manufacturers: [...new Set(catalogCars.map(car => car.manufacturer))].sort()
        };
    }

    function showRateCaptcha(fields, retry) {
        if (!rateCaptchaModal || !rateCaptchaQuestion || !rateCaptchaId) return;
        rateCaptchaQuestion.textContent = fields?.captchaQuestion || 'Решите пример';
        rateCaptchaId.value = fields?.captchaId || '';
        rateCaptchaRetry = retry;
        if (rateCaptchaAnswer) rateCaptchaAnswer.value = '';
        if (rateCaptchaError) rateCaptchaError.textContent = '';
        rateCaptchaModal.classList.add('active');
        rateCaptchaAnswer?.focus();
    }

    async function loadMoreCatalogCars() {
        if (!catalogGrid || !catalogFilterList || catalogLoading || catalogCars.length >= catalogMaxCars) return;

        const activeSources = popularAuctionSources.filter(source => !source.done);
        if (!activeSources.length) {
            if (catalogLoadMore) catalogLoadMore.hidden = true;
            return;
        }

        catalogLoading = true;
        if (catalogLoadMore) {
            catalogLoadMore.hidden = false;
            catalogLoadMore.disabled = true;
            catalogLoadMore.textContent = 'Загружаем ещё автомобили...';
        }
        try {
            const responses = await Promise.allSettled(activeSources.map(async source => {
                const response = await fetch(
                    `${API_BASE}/api/auctions/search?source=${source.value}&limit=${catalogBatchSize}&offset=${source.offset}`
                );
                if (!response.ok) {
                    const payload = await response.json().catch(() => ({}));
                    const error = new Error(payload.message || 'Источник временно недоступен');
                    error.status = response.status;
                    error.fields = payload.fields;
                    throw error;
                }
                const payload = await response.json();
                const items = Array.isArray(payload.items) ? payload.items : [];
                source.offset += catalogBatchSize;
                source.done = items.length < catalogBatchSize;
                return items.map(lot => ({
                    id: lot.id,
                    source: source.value,
                    country: source.country,
                    manufacturer: lot.manufacturer || 'Другое',
                    title: [lot.manufacturer, lot.model].filter(Boolean).join(' ') || `Лот ${lot.id}`,
                    year: lot.year,
                    engine: lot.engine ? `${lot.engine} см³` : '',
                    lot: lot.lot,
                    imageUrl: lot.imageUrl,
                    priceLabel: lot.price ? `${formatPrice(lot.price)} ${source.currency}` : 'Расчёт по запросу',
                    description: [lot.auction, lot.mileage ? `${formatPrice(lot.mileage)} км` : '']
                        .filter(Boolean).join(' · ')
                }));
            }));

            const newCars = responses
                .filter(result => result.status === 'fulfilled')
                .flatMap(result => result.value);
            const captchaFailure = responses.find(result => result.status === 'rejected' && result.reason?.status === 429);
            if (captchaFailure) {
                showRateCaptcha(captchaFailure.reason.fields, loadMoreCatalogCars);
            }
            const known = new Set(catalogCars.map(car => `${car.source}:${car.id}`));
            catalogCars.push(...newCars.filter(car => !known.has(`${car.source}:${car.id}`)));
            if (!catalogCars.length) throw new Error('Нет доступных предложений');

            renderCatalogFilters(catalogFilters());
            renderCatalogCars(catalogCars);
        } catch {
            if (!catalogCars.length && catalogEmpty) {
                catalogGrid.innerHTML = '';
                catalogEmpty.textContent = 'Готовые варианты временно недоступны. Оставьте заявку, менеджер подберёт автомобиль вручную.';
                catalogEmpty.hidden = false;
            }
        } finally {
            catalogLoading = false;
            if (catalogLoadMore) {
                const hasMore = catalogCars.length < catalogMaxCars
                    && popularAuctionSources.some(source => !source.done);
                catalogLoadMore.hidden = !hasMore;
                catalogLoadMore.disabled = false;
                catalogLoadMore.textContent = hasMore ? 'Показать ещё' : 'Все предложения загружены';
            }
        }
    }

    function loadCatalogData() {
        if (!catalogGrid || !catalogFilterList) return;
        catalogGrid.innerHTML = '<div class="catalog-loading">Загружаем актуальные предложения...</div>';
        loadMoreCatalogCars();
    }

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
        document.addEventListener('click', (event) => {
            const button = event.target.closest('.catalog-more');
            if (!button) return;

            if (button.dataset.lotId) {
                openAuctionLot(
                    button.dataset.lotId,
                    button.dataset.lotSource || 'japan',
                    button.dataset.title || 'Аукционный лот'
                );
                return;
            }

            carModalImage.src = button.dataset.image || '';
            carModalCountry.textContent = button.dataset.country || '';
            carModalTitle.textContent = button.dataset.title || '';
            carModalInfo.textContent = button.dataset.info || '';
            carModalPrice.textContent = button.dataset.price || '';
            carModalText.textContent = button.dataset.text || '';
            carModal.classList.add('active');
        });

        carModalClose.addEventListener('click', () => carModal.classList.remove('active'));
        carModal.addEventListener('click', (event) => {
            if (event.target === carModal) carModal.classList.remove('active');
        });
    }

    loadCatalogData();

    if (catalogLoadMore) {
        catalogLoadMore.addEventListener('click', loadMoreCatalogCars);
        if ('IntersectionObserver' in window) {
            const catalogObserver = new IntersectionObserver(entries => {
                if (entries.some(entry => entry.isIntersecting)) {
                    loadMoreCatalogCars();
                }
            }, { rootMargin: '500px 0px' });
            catalogObserver.observe(catalogLoadMore);
        }
    }

    if (rateCaptchaClose && rateCaptchaModal) {
        rateCaptchaClose.addEventListener('click', () => rateCaptchaModal.classList.remove('active'));
    }

    if (rateCaptchaVerify) {
        rateCaptchaVerify.addEventListener('click', async () => {
            const answer = Number(rateCaptchaAnswer?.value);
            if (!rateCaptchaId?.value || !Number.isInteger(answer)) {
                if (rateCaptchaError) rateCaptchaError.textContent = 'Введите ответ на пример.';
                rateCaptchaAnswer?.focus();
                return;
            }
            rateCaptchaVerify.disabled = true;
            if (rateCaptchaError) rateCaptchaError.textContent = '';
            try {
                const response = await fetch(`${API_BASE}/api/auctions/captcha/verify`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ captchaId: rateCaptchaId.value, answer })
                });
                if (!response.ok) throw new Error(await parseApiMessage(response));
                rateCaptchaModal?.classList.remove('active');
                const retry = rateCaptchaRetry;
                rateCaptchaRetry = null;
                if (retry) await retry();
            } catch (error) {
                if (rateCaptchaError) rateCaptchaError.textContent = error.message;
            } finally {
                rateCaptchaVerify.disabled = false;
            }
        });
    }

    const auctionSearchForm = document.getElementById('auctionSearchForm');
    const auctionRefresh = document.getElementById('auctionRefresh');
    const auctionGrid = document.getElementById('auctionGrid');
    const auctionState = document.getElementById('auctionState');
    const auctionLeadModal = document.getElementById('auctionLeadModal');
    const auctionLeadForm = document.getElementById('auctionLeadForm');
    const auctionLeadClose = document.getElementById('auctionLeadClose');
    const auctionLeadTitle = document.getElementById('auctionLeadTitle');
    const auctionLeadLotId = document.getElementById('auctionLeadLotId');
    const auctionLeadLotTitle = document.getElementById('auctionLeadLotTitle');
    const auctionManufacturer = document.getElementById('auctionManufacturer');
    const auctionModel = document.getElementById('auctionModel');
    const auctionSource = document.getElementById('auctionSource');
    const auctionResultsSource = document.getElementById('auctionResultsSource');
    const auctionLotDetails = document.getElementById('auctionLotDetails');
    const auctionLotGallery = document.getElementById('auctionLotGallery');
    const auctionLotFacts = document.getElementById('auctionLotFacts');
    const auctionCaptcha = document.getElementById('auctionCaptcha');
    const auctionCaptchaQuestion = document.getElementById('auctionCaptchaQuestion');
    const auctionCaptchaAnswer = document.getElementById('auctionCaptchaAnswer');
    const auctionCaptchaId = document.getElementById('auctionCaptchaId');
    const auctionCaptchaVerify = document.getElementById('auctionCaptchaVerify');
    let pendingLotRequest = null;

    const auctionSources = {
        japan: { label: 'Японии', currency: '¥' },
        korea: { label: 'Кореи', currency: '₩' },
        china: { label: 'Китая', currency: '¥' }
    };

    function currentAuctionSource() {
        return auctionSource?.value || 'japan';
    }

    function auctionTitle(lot) {
        return [lot.manufacturer, lot.model, lot.year].filter(Boolean).join(' ') || `Лот ${lot.id}`;
    }

    function auctionMeta(lot) {
        return [
            lot.auction,
            lot.lot ? `лот ${lot.lot}` : '',
            lot.grade ? `оценка ${lot.grade}` : '',
            lot.mileage ? `${formatPrice(lot.mileage)} км` : ''
        ].filter(Boolean).join(' · ');
    }

    function setAuctionState(message, type = 'muted') {
        if (!auctionState) return;
        auctionState.textContent = message;
        auctionState.classList.toggle('error', type === 'error');
        auctionState.hidden = false;
    }

    function setSelectOptions(select, values, placeholder) {
        if (!select) return;
        select.innerHTML = `<option value="">${escapeHtml(placeholder)}</option>`
            + values.map(value => `<option value="${escapeHtml(value)}">${escapeHtml(value)}</option>`).join('');
    }

    async function loadAuctionManufacturers() {
        if (!auctionManufacturer) return;
        try {
            const source = currentAuctionSource();
            const response = await fetch(`${API_BASE}/api/auctions/manufacturers?source=${encodeURIComponent(source)}`);
            if (!response.ok) throw new Error(await parseApiMessage(response));
            const values = await response.json();
            setSelectOptions(auctionManufacturer, Array.isArray(values) ? values : [], 'Любая марка');
        } catch {
            setSelectOptions(auctionManufacturer, [], 'Марки временно недоступны');
        }
    }

    async function loadAuctionModels(manufacturer) {
        if (!auctionModel) return;
        if (!manufacturer) {
            auctionModel.disabled = true;
            setSelectOptions(auctionModel, [], 'Сначала выберите марку');
            return;
        }
        auctionModel.disabled = true;
        setSelectOptions(auctionModel, [], 'Загружаем модели...');
        try {
            const source = currentAuctionSource();
            const response = await fetch(`${API_BASE}/api/auctions/models?source=${encodeURIComponent(source)}&manufacturer=${encodeURIComponent(manufacturer)}`);
            if (!response.ok) throw new Error(await parseApiMessage(response));
            const values = await response.json();
            setSelectOptions(auctionModel, Array.isArray(values) ? values : [], 'Любая модель');
            auctionModel.disabled = false;
        } catch {
            setSelectOptions(auctionModel, [], 'Модели временно недоступны');
        }
    }

    function renderAuctionLots(items, source) {
        if (!auctionGrid) return;

        if (!items.length) {
            auctionGrid.innerHTML = '';
            setAuctionState('По таким параметрам лоты не найдены.');
            return;
        }

        if (auctionState) {
            auctionState.hidden = true;
        }

        auctionGrid.innerHTML = items.map(lot => `
            <article class="auction-card">
                <div class="auction-card-media">
                    ${lot.imageUrl
                        ? `<img src="${escapeHtml(lot.imageUrl)}" alt="${escapeHtml(auctionTitle(lot))}">`
                        : `<div class="auction-card-placeholder">Фото появится после загрузки данных</div>`}
                </div>
                <div class="auction-card-body">
                    <span>${escapeHtml(lot.auction || 'Аукцион')}</span>
                    <h3>${escapeHtml(auctionTitle(lot))}</h3>
                    <p>${escapeHtml(auctionMeta(lot) || 'Данные лота уточняются')}</p>
                    <dl>
                        <div><dt>Цена</dt><dd>${lot.price ? `${formatPrice(lot.price)} ¥` : 'по запросу'}</dd></div>
                        <div><dt>Цвет</dt><dd>${escapeHtml(lot.color || '-')}</dd></div>
                        <div><dt>Двигатель</dt><dd>${escapeHtml(lot.engine || '-')}</dd></div>
                    </dl>
                    <button
                        class="auction-lead-open"
                        data-lot-id="${escapeHtml(lot.id)}"
                        data-lot-title="${escapeHtml(auctionTitle(lot))}"
                        data-lot-source="${escapeHtml(source)}">
                        Подробнее о лоте
                    </button>
                </div>
            </article>
        `).join('');

        auctionGrid.querySelectorAll('.auction-lead-open').forEach(button => {
            button.addEventListener('click', () => openAuctionLot(
                button.dataset.lotId || '',
                button.dataset.lotSource || 'japan',
                button.dataset.lotTitle || 'Аукционный лот'
            ));
        });
    }

    function renderAuctionLotDetails(lot, source) {
        if (!auctionLotDetails || !auctionLotGallery || !auctionLotFacts) return;
        const images = Array.isArray(lot.imageUrls) && lot.imageUrls.length
            ? lot.imageUrls
            : (lot.imageUrl ? [lot.imageUrl.replace('&w=320', '')] : []);
        auctionLotGallery.innerHTML = images.length
            ? images.slice(0, 8).map(image => `
                <a href="${escapeHtml(image)}" target="_blank" rel="noopener" title="Открыть фотографию в полном размере">
                    <img src="${escapeHtml(image + (image.includes('7.tru.ru/imgs/') && !image.includes('&w=') ? '&w=320' : ''))}"
                         alt="${escapeHtml(auctionTitle(lot))}">
                    <span>Увеличить</span>
                </a>
            `).join('')
            : '<div class="auction-card-placeholder">Фотографии для этого лота пока не переданы</div>';

        const currency = auctionSources[source]?.currency || '';
        const facts = [
            ['Аукцион', lot.auction],
            ['Номер лота', lot.lot || lot.id],
            ['Дата торгов', lot.auctionDate],
            ['Год', lot.year],
            ['Пробег', lot.mileage ? `${formatPrice(lot.mileage)} км` : ''],
            ['Оценка', lot.grade],
            ['Цвет', lot.color],
            ['Двигатель', lot.engine],
            ['Цена', lot.price ? `${formatPrice(lot.price)} ${currency}` : 'по запросу']
        ].filter(([, value]) => value !== null && value !== undefined && value !== '');
        auctionLotFacts.innerHTML = facts.map(([label, value]) => `
            <div><dt>${escapeHtml(label)}</dt><dd>${escapeHtml(String(value))}</dd></div>
        `).join('');
        auctionLotDetails.hidden = false;
    }

    function showAuctionCaptcha(fields) {
        if (!auctionCaptcha || !auctionCaptchaQuestion || !auctionCaptchaId) return;
        auctionCaptchaId.value = fields?.captchaId || '';
        auctionCaptchaQuestion.textContent = fields?.captchaQuestion || 'Решите пример';
        auctionCaptcha.hidden = false;
        if (auctionCaptchaAnswer) {
            auctionCaptchaAnswer.value = '';
            auctionCaptchaAnswer.focus();
        }
        const submit = auctionLeadForm?.querySelector('button[type="submit"]');
        if (submit) submit.disabled = true;
    }

    async function openAuctionLot(id, source, fallbackTitle) {
        if (!id || !auctionLeadModal || !auctionLeadTitle || !auctionLeadLotId || !auctionLeadLotTitle) return;
        pendingLotRequest = { id, source, fallbackTitle };
        auctionLeadModal.classList.add('active');
        auctionLeadTitle.textContent = 'Загружаем данные лота...';
        auctionLeadLotId.value = id;
        auctionLeadLotTitle.value = fallbackTitle;
        if (auctionLotDetails) auctionLotDetails.hidden = true;
        if (auctionCaptcha) auctionCaptcha.hidden = true;

        try {
            const response = await fetch(`${API_BASE}/api/auctions/${encodeURIComponent(id)}?source=${encodeURIComponent(source)}`);
            const payload = await response.json().catch(() => ({}));
            if (response.status === 429) {
                auctionLeadTitle.textContent = fallbackTitle;
                showAuctionCaptcha(payload.fields);
                return;
            }
            if (!response.ok) {
                throw new Error(payload.message || 'Не удалось загрузить данные лота');
            }
            auctionLeadTitle.textContent = auctionTitle(payload);
            auctionLeadLotTitle.value = auctionTitle(payload);
            renderAuctionLotDetails(payload, source);
            const submit = auctionLeadForm?.querySelector('button[type="submit"]');
            if (submit) submit.disabled = false;
        } catch (error) {
            auctionLeadTitle.textContent = fallbackTitle;
            if (auctionLotFacts) {
                auctionLotFacts.innerHTML = `<div><dt>Статус</dt><dd>${escapeHtml(error.message)}</dd></div>`;
            }
            if (auctionLotDetails) auctionLotDetails.hidden = false;
        }
    }

    async function searchAuctions() {
        if (!auctionSearchForm || !auctionGrid) return;

        const formData = new FormData(auctionSearchForm);
        const params = new URLSearchParams();
        ['source', 'query', 'manufacturer', 'model', 'yearFrom', 'yearTo', 'maxMileage', 'lotNumber', 'dayOfWeek'].forEach(name => {
            const value = String(formData.get(name) || '').trim();
            if (value) {
                params.set(name, value);
            }
        });
        params.set('limit', '20');

        const button = auctionSearchForm.querySelector('button[type="submit"]');
        const previousText = button ? button.textContent : '';
        if (button) {
            button.disabled = true;
            button.textContent = 'Ищем...';
        }
        setAuctionState('Загружаем лоты...');

        try {
            const response = await fetch(`${API_BASE}/api/auctions/search?${params.toString()}`);
            if (response.status === 429) {
                const payload = await response.json().catch(() => ({}));
                showRateCaptcha(payload.fields, searchAuctions);
                setAuctionState('Подтвердите, что вы человек, чтобы продолжить поиск.');
                return;
            }
            if (!response.ok) {
                const message = await parseApiMessage(response);
                throw new Error(message.includes('API-код')
                    ? 'Аукционные лоты временно недоступны. Оставьте заявку, менеджер подберёт варианты вручную.'
                    : message);
            }
            const result = await response.json();
            renderAuctionLots(Array.isArray(result.items) ? result.items : [], result.source || currentAuctionSource());
        } catch (error) {
            auctionGrid.innerHTML = '';
            setAuctionState(error.message || 'Не удалось загрузить аукционные лоты', 'error');
        } finally {
            if (button) {
                button.disabled = false;
                button.textContent = previousText;
            }
        }
    }

    if (auctionSearchForm) {
        auctionSearchForm.addEventListener('submit', (event) => {
            event.preventDefault();
            if (auctionSearchForm.reportValidity()) {
                searchAuctions();
            }
        });
    }

    if (auctionManufacturer) {
        auctionManufacturer.addEventListener('change', () => {
            loadAuctionModels(auctionManufacturer.value);
        });
        loadAuctionManufacturers();
    }

    if (auctionSource) {
        auctionSource.addEventListener('change', () => {
            const source = currentAuctionSource();
            if (auctionResultsSource) {
                auctionResultsSource.textContent = `Аукционы ${auctionSources[source]?.label || ''}`;
            }
            setSelectOptions(auctionModel, [], 'Сначала выберите марку');
            if (auctionModel) auctionModel.disabled = true;
            loadAuctionManufacturers();
            setAuctionState('Настройте параметры и запустите поиск.');
            if (auctionGrid) auctionGrid.innerHTML = '';
        });
    }

    if (auctionCaptchaVerify) {
        auctionCaptchaVerify.addEventListener('click', async () => {
            if (!auctionCaptchaId || !auctionCaptchaAnswer || !pendingLotRequest) return;
            const answer = Number(auctionCaptchaAnswer.value);
            if (!Number.isInteger(answer)) {
                auctionCaptchaAnswer.focus();
                return;
            }
            auctionCaptchaVerify.disabled = true;
            try {
                const response = await fetch(`${API_BASE}/api/auctions/captcha/verify`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ captchaId: auctionCaptchaId.value, answer })
                });
                if (!response.ok) throw new Error(await parseApiMessage(response));
                await openAuctionLot(pendingLotRequest.id, pendingLotRequest.source, pendingLotRequest.fallbackTitle);
            } catch (error) {
                if (auctionCaptchaQuestion) auctionCaptchaQuestion.textContent = error.message;
            } finally {
                auctionCaptchaVerify.disabled = false;
            }
        });
    }

    if (auctionRefresh) {
        auctionRefresh.addEventListener('click', searchAuctions);
    }

    if (auctionLeadClose && auctionLeadModal) {
        auctionLeadClose.addEventListener('click', () => auctionLeadModal.classList.remove('active'));
        auctionLeadModal.addEventListener('click', (event) => {
            if (event.target === auctionLeadModal) {
                auctionLeadModal.classList.remove('active');
            }
        });
    }

    if (auctionLeadForm) {
        auctionLeadForm.addEventListener('submit', (event) => {
            event.preventDefault();
            if (!auctionLeadForm.reportValidity()) {
                return;
            }

            const formData = new FormData(auctionLeadForm);
            submitJsonForm(auctionLeadForm, '/api/auctions/leads', {
                lotId: String(formData.get('lotId') || '').trim(),
                lotTitle: String(formData.get('lotTitle') || '').trim(),
                name: String(formData.get('name') || '').trim(),
                phone: String(formData.get('phone') || '').trim(),
                comment: String(formData.get('comment') || '').trim(),
                policyAccepted: formData.get('policyAccepted') === 'on'
            });
        });
    }

});
