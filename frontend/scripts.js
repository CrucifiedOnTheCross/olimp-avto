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
    let catalogCars = [];
    let catalogFilter = { type: 'all', value: 'all' };

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
            <button class="catalog-category active" data-filter-type="all" data-filter-value="all">Все</button>
            ${countries.map(country => `
                <button class="catalog-category" data-filter-type="country" data-filter-value="${escapeHtml(country)}">
                    ${escapeHtml(country)}
                </button>
            `).join('')}
            ${manufacturers.length ? `<div class="catalog-filter-subtitle">Марки автомобилей</div>` : ''}
            ${manufacturers.map(manufacturer => `
                <button class="catalog-category" data-filter-type="manufacturer" data-filter-value="${escapeHtml(manufacturer)}">
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
                return carManufacturer(car.title) === catalogFilter.value;
            }
            return true;
        });

        catalogGrid.innerHTML = filtered.map(car => {
            const image = car.imageUrl && !car.imageUrl.includes('car-')
                ? car.imageUrl
                : 'images/car-hero.png';
            const info = [car.year, car.engine].filter(Boolean).join(' · ');
            return `
                <article class="catalog-car" data-country="${escapeHtml(car.country || '')}">
                    <div class="catalog-car-img">
                        <img src="${escapeHtml(image)}" alt="${escapeHtml(car.title || 'Автомобиль')}" onerror="this.src='images/car-hero.png'">
                        <span>${escapeHtml(car.country || 'Авто')}</span>
                    </div>
                    <div class="catalog-car-body">
                        <h3>${escapeHtml(car.title || 'Автомобиль')}</h3>
                        <p>${escapeHtml(info || car.description || 'Подробности уточним при расчёте')}</p>
                        <strong>${car.price ? `от ${formatPrice(car.price)} ₽` : 'Цена по запросу'}</strong>
                        <button
                            class="catalog-more"
                            data-title="${escapeHtml(car.title || 'Автомобиль')}"
                            data-country="${escapeHtml(car.country || '')}"
                            data-price="${car.price ? `от ${formatPrice(car.price)} ₽` : 'Цена по запросу'}"
                            data-image="${escapeHtml(image)}"
                            data-info="${escapeHtml(info)}"
                            data-text="${escapeHtml(car.description || 'Оставьте заявку, и менеджер подготовит расчёт под ключ.')}">
                            Подробнее
                        </button>
                    </div>
                </article>
            `;
        }).join('');

        if (catalogEmpty) {
            catalogEmpty.hidden = filtered.length > 0;
        }
    }

    async function loadCatalogData() {
        if (!catalogGrid || !catalogFilterList) return;

        try {
            const [carsResponse, filtersResponse] = await Promise.all([
                fetch(`${API_BASE}/api/cars`),
                fetch(`${API_BASE}/api/cars/filters`)
            ]);

            if (!carsResponse.ok || !filtersResponse.ok) {
                throw new Error('Не удалось загрузить витрину');
            }

            catalogCars = await carsResponse.json();
            renderCatalogFilters(await filtersResponse.json());
            renderCatalogCars(Array.isArray(catalogCars) ? catalogCars : []);
        } catch {
            catalogGrid.innerHTML = '';
            if (catalogEmpty) {
                catalogEmpty.textContent = 'Готовые варианты временно недоступны. Оставьте заявку, менеджер подберёт автомобиль вручную.';
                catalogEmpty.hidden = false;
            }
        }
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
            const response = await fetch(`${API_BASE}/api/auctions/manufacturers`);
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
            const response = await fetch(`${API_BASE}/api/auctions/models?manufacturer=${encodeURIComponent(manufacturer)}`);
            if (!response.ok) throw new Error(await parseApiMessage(response));
            const values = await response.json();
            setSelectOptions(auctionModel, Array.isArray(values) ? values : [], 'Любая модель');
            auctionModel.disabled = false;
        } catch {
            setSelectOptions(auctionModel, [], 'Модели временно недоступны');
        }
    }

    function renderAuctionLots(items) {
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
                        data-lot-title="${escapeHtml(auctionTitle(lot))}">
                        Рассчитать под ключ
                    </button>
                </div>
            </article>
        `).join('');

        auctionGrid.querySelectorAll('.auction-lead-open').forEach(button => {
            button.addEventListener('click', () => {
                if (!auctionLeadModal || !auctionLeadTitle || !auctionLeadLotId || !auctionLeadLotTitle) return;
                auctionLeadTitle.textContent = button.dataset.lotTitle || 'Аукционный лот';
                auctionLeadLotId.value = button.dataset.lotId || '';
                auctionLeadLotTitle.value = button.dataset.lotTitle || '';
                auctionLeadModal.classList.add('active');
            });
        });
    }

    async function searchAuctions() {
        if (!auctionSearchForm || !auctionGrid) return;

        const formData = new FormData(auctionSearchForm);
        const params = new URLSearchParams();
        ['query', 'manufacturer', 'model', 'yearFrom', 'yearTo', 'maxMileage'].forEach(name => {
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
            if (!response.ok) {
                const message = await parseApiMessage(response);
                throw new Error(message.includes('API-код')
                    ? 'Аукционные лоты временно недоступны. Оставьте заявку, менеджер подберёт варианты вручную.'
                    : message);
            }
            const result = await response.json();
            renderAuctionLots(Array.isArray(result.items) ? result.items : []);
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
