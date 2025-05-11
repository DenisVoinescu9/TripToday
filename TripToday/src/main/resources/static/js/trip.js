function calculateAndUpdateDuration(departureDateInput, returnDateInput, durationInput) {
    const departureDateStr = $(departureDateInput).val();
    const returnDateStr = $(returnDateInput).val();
    const durationField = $(durationInput);
    const departureDateElem = $(departureDateInput)[0];
    const returnDateElem = $(returnDateInput)[0];

    durationField.removeClass('is-invalid is-valid');
    $(returnDateInput).removeClass('is-invalid is-valid');
    $(departureDateInput).removeClass('is-invalid is-valid');

    let isDepartureIndividuallyValid = departureDateElem.checkValidity();
    let isReturnIndividuallyValid = returnDateElem.checkValidity();

    if (!departureDateStr || !isDepartureIndividuallyValid) {
        $(departureDateInput).addClass('is-invalid');
        isDepartureIndividuallyValid = false;
    } else {
        $(departureDateInput).addClass('is-valid');
    }
    if (!returnDateStr || !isReturnIndividuallyValid) {
        $(returnDateInput).addClass('is-invalid');
        isReturnIndividuallyValid = false;
    } else {
        $(returnDateInput).addClass('is-valid');
    }

    if (isDepartureIndividuallyValid && isReturnIndividuallyValid && departureDateStr && returnDateStr) {
        const departureDate = new Date(departureDateStr);
        const returnDate = new Date(returnDateStr);

        if (returnDate >= departureDate) {
            const timeDiff = returnDate.getTime() - departureDate.getTime();
            const durationDays = Math.ceil(timeDiff / (1000 * 60 * 60 * 24)) + 1;
            durationField.val(durationDays >= 1 ? durationDays : 1).addClass('is-valid');
        } else {
            durationField.val('').addClass('is-invalid');
            $(returnDateInput).removeClass('is-valid').addClass('is-invalid');
            $(departureDateInput).removeClass('is-valid').addClass('is-invalid');
        }
    } else {
        durationField.val('').addClass('is-invalid');
    }
}

function validateFormField(fieldElement) {
    const field = $(fieldElement);
    const fieldName = field.attr('name');
    const fieldType = field.attr('type');
    const tagName = fieldElement.tagName.toLowerCase();

    fieldElement.setCustomValidity("");
    field.removeClass('is-valid is-invalid');

    if (fieldName === 'expirationDate') {
        const fieldValue = field.val();
        if (fieldValue) {
            const datePattern = /^(0[1-9]|1[0-2])\/(\d{2})$/;
            const match = fieldValue.match(datePattern);
            if (!match) {
                fieldElement.setCustomValidity("Formatul trebuie să fie MM/YY.");
            } else {
                const expiryMonth = parseInt(match[1], 10);
                const expiryYearSuffix = parseInt(match[2], 10);
                const expiryYear = 2000 + expiryYearSuffix;
                const today = new Date();
                const currentYear = today.getFullYear();
                const currentMonth = today.getMonth() + 1;
                if (expiryYear < currentYear || (expiryYear === currentYear && expiryMonth < currentMonth)) {
                    fieldElement.setCustomValidity("Data de expirare este în trecut.");
                } else {
                    fieldElement.setCustomValidity("");
                }
            }
        } else if (fieldElement.required) {
            // Lasă validarea HTML5 'required' să își seteze mesajul default
        } else {
            fieldElement.setCustomValidity("");
        }
    } else if (fieldElement.required) {
        if (fieldType === 'text' || fieldType === 'url' || fieldType === 'email' || fieldType === 'password' || tagName === 'textarea') {
            if (field.val().trim() === "") {
                fieldElement.setCustomValidity("Acest câmp este obligatoriu și nu poate conține doar spații.");
            }
        }
    }

    let isFieldFinallyValid = fieldElement.checkValidity();

    if (isFieldFinallyValid) {
        field.addClass('is-valid');
    } else {
        field.addClass('is-invalid');
    }
    return isFieldFinallyValid;
}

function validateForm(formElement) {
    const form = $(formElement);
    let isFormCompletelyValid = true;
    form.find('input[required], select[required], textarea[required]').each(function () {
        if (this.id !== 'addDurationDays' && this.id !== 'editDurationDays') {
            if (!validateFormField(this)) {
                isFormCompletelyValid = false;
            }
        }
    });
    if (form.attr('id') === 'addTripForm' || form.attr('id') === 'editTripForm') {
        const departureInputId = form.attr('id') === 'addTripForm' ? '#addDepartureDate' : '#editDepartureDate';
        const returnInputId = form.attr('id') === 'addTripForm' ? '#addReturnDate' : '#editReturnDate';
        const durationInputId = form.attr('id') === 'addTripForm' ? '#addDurationDays' : '#editDurationDays';
        let departureValid = validateFormField($(departureInputId)[0]);
        let returnValid = validateFormField($(returnInputId)[0]);
        if (departureValid && returnValid) {
            calculateAndUpdateDuration(departureInputId, returnInputId, durationInputId);
            if ($(durationInputId).hasClass('is-invalid')) { isFormCompletelyValid = false; }
        } else {
            $(durationInputId).val('').removeClass('is-valid').addClass('is-invalid');
            isFormCompletelyValid = false;
        }
        if (!departureValid || !returnValid || $(durationInputId).hasClass('is-invalid')) {
            isFormCompletelyValid = false;
        }
    }
    return isFormCompletelyValid;
}

function clearFormValidation(formElement) {
    const form = $(formElement);
    form.removeClass('was-validated');
    form.find('.is-valid, .is-invalid').removeClass('is-valid is-invalid');
    form.find('input, select, textarea').each(function() {
        if (typeof this.setCustomValidity === 'function') {
            this.setCustomValidity("");
        }
    });
}

function openAddTripModal() {
    const form = $('#addTripForm');
    form[0].reset();
    clearFormValidation(form);
    $('#addDurationDays').val('');
    const today = new Date();
    const tomorrow = new Date(today);
    tomorrow.setDate(today.getDate() + 1);
    const tomorrowString = tomorrow.toISOString().split('T')[0];
    $('#addDepartureDate').attr('min', tomorrowString);
    $('#addReturnDate').attr('min', tomorrowString);
    $('#addTripModal').modal('show');
}

function openEditTripModal(button) {
    const form = $('#editTripForm');
    form[0].reset();
    clearFormValidation(form);

    const tripId = $(button).data('tripid');
    const card = $(button).closest('.trip-card');
    const destination = card.find('.trip-column-image p').text().trim();
    const detailsItems = card.find('.trip-wrapper-details-item');
    const departureLocation = detailsItems.eq(0).find('p').text().trim();
    const departureDateISO = card.data('departure-date-iso');
    const departureHourRaw = detailsItems.eq(2).find('p').text().trim();
    const returnDateISO = card.data('return-date-iso');
    const availableSpotsRaw = detailsItems.eq(5).find('p').text().trim();
    const registrationFeeRaw = detailsItems.eq(6).find('p').text().trim();
    const hotelNameText = detailsItems.eq(7).find('p').text().trim();
    const guideIdFromCard = card.data('guide-id');

    let departureHour = '';
    if (departureHourRaw) {
        const timeMatch = departureHourRaw.match(/(\d{1,2}:\d{2})/);
        if (timeMatch && timeMatch[1]) {
            let parts = timeMatch[1].split(':');
            if (parts[0].length === 1) {
                parts[0] = '0' + parts[0];
            }
            if (parts[1] && parts[1].length === 1) {
                parts[1] = '0' + parts[1];
            }
            departureHour = parts.join(':');
        }
    }

    const availableSpots = availableSpotsRaw.replace(/\D/g, '');
    const registrationFee = registrationFeeRaw.replace(/[^\d.-]/g, '');
    const hotelName = (hotelNameText === 'No guide assigned' || hotelNameText === 'Not specified') ? '' : hotelNameText;
    const description = card.find('.trip-wrapper-description p').text().trim();
    const pictureUrl = card.find('.trip-column-image img').attr('src');

    $('#editTripId').val(tripId);
    $('#editDestination').val(destination);
    $('#editDepartureLocation').val(departureLocation);
    $('#editDepartureDate').val(departureDateISO);
    $('#editDepartureHour').val(departureHour);
    $('#editReturnDate').val(returnDateISO);
    $('#editAvailableSpots').val(availableSpots);
    $('#editRegistrationFee').val(registrationFee);
    $('#editDescription').val(description);
    $('#editHotelName').val(hotelName);
    $('#editPicture').val(pictureUrl);
    $('#editGuideSelect').val(guideIdFromCard || '');

    const today = new Date();
    const tomorrow = new Date(today);
    tomorrow.setDate(today.getDate() + 1);
    const tomorrowString = tomorrow.toISOString().split('T')[0];
    $('#editDepartureDate').attr('min', tomorrowString);
    $('#editReturnDate').attr('min', departureDateISO || tomorrowString);

    if (departureDateISO && returnDateISO) {
        calculateAndUpdateDuration('#editDepartureDate', '#editReturnDate', '#editDurationDays');
    } else {
        $('#editDurationDays').val('').removeClass('is-valid is-invalid');
        validateFormField($('#editDurationDays')[0]);
    }

    $('#editTripForm').find('input[required], select[required], textarea[required]').each(function() {
        if (this.id !== 'editDurationDays') {
            validateFormField(this);
        }
    });

    $('#editTripForm').addClass('was-validated');
    $('#editTripModal').modal('show');
}

function openDeleteModal(button) {
    const tripId = $(button).data('tripid');
    $('#deleteTripId').val(tripId);
    $('#deleteTripModal').modal('show');
}

function openViewTravelersModal(buttonElement) {
    const tripId = buttonElement.getAttribute('data-tripid');
    const listElement = document.getElementById('enrolledTravelersList');
    listElement.innerHTML = '<div class="text-center"><div class="spinner-border" role="status"><span class="sr-only">Loading...</span></div></div>';
    $('#viewTravelersModal').modal('show');
    const apiUrl = `/api/v2/user-trips/trip/${tripId}/details`;
    fetch(apiUrl)
        .then(response => {
            if (!response.ok) { return response.text().then(text => { throw new Error(`HTTP error! Status: ${response.status}, Message: ${text || 'No details provided by server.'}`); }); }
            const contentType = response.headers.get("content-type");
            if (contentType && contentType.indexOf("application/json") !== -1) { return response.json(); } else { return []; }
        })
        .then(travelers => {
            listElement.innerHTML = '';
            if (travelers && travelers.length > 0) {
                const listGroup = document.createElement('div'); listGroup.className = 'list-group';
                travelers.forEach(traveler => {
                    const travelerItem = document.createElement('div'); travelerItem.className = 'list-group-item d-flex align-items-center';
                    const img = document.createElement('img'); img.src = traveler.picture || '/img/default-avatar.png'; img.alt = 'Traveler picture'; img.width = 65; img.height = 65; img.className = 'rounded-circle mr-2';
                    travelerItem.appendChild(img);
                    const textWrapper = document.createElement('div');
                    const spanId = document.createElement('span'); spanId.textContent = traveler.user_id || 'Unknown User ID'; spanId.style.display = 'block'; spanId.style.fontWeight = '500'; spanId.style.wordBreak = 'break-all';
                    const spanName = document.createElement('span'); let nameOrEmail = traveler.name || traveler.email || ''; if (!nameOrEmail) { nameOrEmail = '(No name/email available)'; } spanName.textContent = nameOrEmail; spanName.style.display = 'block'; spanName.style.fontSize = '0.9em'; spanName.style.color = '#6c757d'; spanName.style.wordBreak = 'break-word';
                    textWrapper.appendChild(spanId); textWrapper.appendChild(spanName);
                    travelerItem.appendChild(textWrapper); listGroup.appendChild(travelerItem);
                });
                listElement.appendChild(listGroup);
            } else { listElement.innerHTML = '<p>No travelers currently enrolled in this trip.</p>'; }
        })
        .catch(error => { listElement.innerHTML = `<p class="text-danger">Could not load traveler list. Please try again later. (${error.message})</p>`; });
}

$(document).ready(function () {
    $('.enroll-button').each(function () {
        const button = $(this);
        const spots = parseInt(button.data('spots'), 10);
        const isGuide = button.data('is-guide') === true || button.data('is-guide') === 'true';
        let popoverContent = '';
        if (isNaN(spots) || spots <= 0) { popoverContent = 'Sorry, there are no available spots left.'; }
        else if (isGuide) { popoverContent = "You can't enroll, you are the guide for this trip."; }
        if (popoverContent) { button.popover({ content: popoverContent, trigger: 'hover focus', placement: 'top', container: 'body' }); }
        else { if (button.data('bs.popover')) { button.popover('dispose'); } }
    });

    $('.enroll-button').on('click', function (event) {
        const button = $(this);
        const spots = parseInt(button.data('spots'), 10);
        const isGuide = button.data('is-guide') === true || button.data('is-guide') === 'true';
        const tripId = button.data('tripid');
        const destination = button.data('destination');
        let popoverMessage = '';
        if (isNaN(spots) || spots <= 0) { popoverMessage = 'Sorry, there are no available spots left.'; }
        else if (isGuide) { popoverMessage = "You can't enroll, you are the guide for this trip."; }
        if (popoverMessage) {
            if (!button.data('bs.popover') || button.data('bs.popover').config.trigger !== 'manual') {
                if (button.data('bs.popover')) button.popover('dispose');
                button.popover({ content: popoverMessage, trigger: 'manual', placement: 'top', container: 'body' });
            } else { button.data('bs.popover').config.content = popoverMessage; }
            button.popover('show');
            setTimeout(function () { button.popover('hide'); }, 2500);
            return;
        }
        if (button.data('bs.popover')) { button.popover('dispose'); }
        const enrollModal = $('#enrollModal');
        const enrollForm = $('#enrollForm');
        clearFormValidation(enrollForm);
        enrollForm[0].reset();
        enrollModal.find('#modalTripId').val(tripId);
        enrollModal.find('#modalDestination').text(destination);
        enrollModal.modal('show');
    });

    $('body').on('click', function (e) {
        $('.enroll-button').each(function () {
            if (!$(this).is(e.target) && $(this).has(e.target).length === 0 && $('.popover').has(e.target).length === 0) {
                if ($(this).data('bs.popover')) { $(this).popover('hide'); }
            }
        });
    });

    $('#addTripForm').find('input[required], select[required], textarea[required]').on('blur', function () {
        if (this.id !== 'addDurationDays') { validateFormField(this); }
        if (this.id === 'addDepartureDate' || this.id === 'addReturnDate') {
            const departureDateVal = $('#addDepartureDate').val();
            if (this.id === 'addDepartureDate' && departureDateVal && this.checkValidity()) {
                $('#addReturnDate').attr('min', departureDateVal);
                validateFormField($('#addReturnDate')[0]);
            }
            calculateAndUpdateDuration('#addDepartureDate', '#addReturnDate', '#addDurationDays');
        }
    });

    $('#editTripForm').find('input[required], select[required], textarea[required]').on('blur', function () {
        if (this.id !== 'editDurationDays') { validateFormField(this); }
        if (this.id === 'editDepartureDate' || this.id === 'editReturnDate') {
            const departureDateVal = $('#editDepartureDate').val();
            if (this.id === 'editDepartureDate' && departureDateVal && this.checkValidity()) {
                $('#editReturnDate').attr('min', departureDateVal);
                validateFormField($('#editReturnDate')[0]);
            }
            calculateAndUpdateDuration('#editDepartureDate', '#editReturnDate', '#editDurationDays');
        }
    });

    $('#enrollForm').find('input[required]').on('blur', function () {
        validateFormField(this);
    });

    $('#enrollForm input[name="expirationDate"]').on('input', function (e) {
        var input = $(this), value = input.val(), digits = value.replace(/\D/g, ''), formattedValue = digits;
        if (digits.length > 2) {
            formattedValue = digits.substring(0, 2) + '/' + digits.substring(2, 4);
        } else if (digits.length === 2 && value.length === 3 && value.endsWith('/')) {
            formattedValue = digits;
        }
        input.val(formattedValue);
        validateFormField(this);
    });

    $('#addDepartureDate, #addReturnDate').on('input change', function () {
        const departureDateVal = $('#addDepartureDate').val();
        if (this.id === 'addDepartureDate' && departureDateVal && this.checkValidity()) {
            $('#addReturnDate').attr('min', departureDateVal);
            validateFormField($('#addReturnDate')[0]);
        }
        calculateAndUpdateDuration('#addDepartureDate', '#addReturnDate', '#addDurationDays');
    });

    $('#editDepartureDate, #editReturnDate').on('input change', function () {
        const departureDateVal = $('#editDepartureDate').val();
        if (this.id === 'editDepartureDate' && departureDateVal && this.checkValidity()) {
            $('#editReturnDate').attr('min', departureDateVal);
            validateFormField($('#editReturnDate')[0]);
        }
        calculateAndUpdateDuration('#editDepartureDate', '#editReturnDate', '#editDurationDays');
    });

    $('.needs-validation').submit(function (event) {
        const form = this;
        const isFormValid = validateForm(form);
        if (!isFormValid) {
            event.preventDefault();
            event.stopPropagation();
            $(form).find('.is-invalid').first().focus();
        }
        $(form).addClass('was-validated');
    });

    const showUpcomingBtn = document.getElementById('show-upcoming-trips-btn');
    const showPastBtn = document.getElementById('show-past-trips-btn');
    const upcomingTripsContent = document.getElementById('upcoming-trips-section');
    const pastTripsContent = document.getElementById('past-trips-section');
    function updateView(showPast) {
        if (!showUpcomingBtn || !showPastBtn || !upcomingTripsContent || !pastTripsContent) { return; }
        if (showPast) {
            pastTripsContent.style.display = 'block'; upcomingTripsContent.style.display = 'none';
            showPastBtn.classList.add('active'); showUpcomingBtn.classList.remove('active');
        } else {
            upcomingTripsContent.style.display = 'block'; pastTripsContent.style.display = 'none';
            showUpcomingBtn.classList.add('active'); showPastBtn.classList.remove('active');
        }
    }
    function checkInitialView() {
        const urlParams = new URLSearchParams(window.location.search);
        const pastPageParam = urlParams.get('pa_page'); const upcomingPageParam = urlParams.get('page');
        let shouldShowPast = false;
        if (pastPageParam !== null) {
            shouldShowPast = true;
            if (upcomingPageParam !== null && upcomingPageParam !== '0') {
                if (urlParams.has('page') && !urlParams.has('pa_page_navigated_explicitly')) { shouldShowPast = false; }
            }
        } else { shouldShowPast = false; }
        updateView(shouldShowPast);
    }
    if (showUpcomingBtn && showPastBtn && upcomingTripsContent && pastTripsContent) {
        checkInitialView();
        showUpcomingBtn.addEventListener('click', function (e) {
            e.preventDefault(); updateView(false);
            const url = new URL(window.location); const currentPage = url.searchParams.get('page') || '0';
            let preservedParams = new URLSearchParams(); preservedParams.set('page', currentPage);
            history.pushState({}, '', url.pathname + '?' + preservedParams.toString());
        });
        showPastBtn.addEventListener('click', function (e) {
            e.preventDefault(); updateView(true);
            const url = new URL(window.location); const currentPastPage = url.searchParams.get('pa_page') || '0';
            let preservedParams = new URLSearchParams(); preservedParams.set('pa_page', currentPastPage);
            history.pushState({}, '', url.pathname + '?' + preservedParams.toString());
        });
    }
});