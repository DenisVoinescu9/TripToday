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
    field.removeClass('is-valid is-invalid');
    let isValid = fieldElement.checkValidity();

    if (field.attr('name') === 'expirationDate' && isValid) {
        const datePattern = /^(0[1-9]|1[0-2])\/(\d{2})$/;
        const match = field.val().match(datePattern);
        if (!match) {
            isValid = false;
        } else {
            const expiryMonth = parseInt(match[1], 10);
            const expiryYearSuffix = parseInt(match[2], 10);
            const expiryYear = 2000 + expiryYearSuffix;
            const today = new Date();
            const currentYear = today.getFullYear();
            const currentMonth = today.getMonth() + 1;
            if (expiryYear < currentYear || (expiryYear === currentYear && expiryMonth < currentMonth)) {
                isValid = false;
            }
        }
    }
    field.addClass(isValid ? 'is-valid' : 'is-invalid');
}

function validateForm(formElement) {
    const form = $(formElement);
    let isFormValid = true;
    form.find('input, select, textarea').filter('[required]').each(function() {
        if (this.id !== 'addDurationDays' && this.id !== 'editDurationDays') {
            validateFormField(this);
            if ($(this).hasClass('is-invalid')) isFormValid = false;
        }
    });
    if (form.attr('id') === 'addTripForm' || form.attr('id') === 'editTripForm') {
        const departureInputId = form.attr('id') === 'addTripForm' ? '#addDepartureDate' : '#editDepartureDate';
        const returnInputId = form.attr('id') === 'addTripForm' ? '#addReturnDate' : '#editReturnDate';
        const durationInputId = form.attr('id') === 'addTripForm' ? '#addDurationDays' : '#editDurationDays';
        validateFormField($(departureInputId)[0]);
        validateFormField($(returnInputId)[0]);
        if ($(departureInputId).hasClass('is-valid') && $(returnInputId).hasClass('is-valid')) {
            calculateAndUpdateDuration(departureInputId, returnInputId, durationInputId);
        } else {
            $(durationInputId).val('').removeClass('is-valid').addClass('is-invalid');
        }
        if ($(departureInputId).hasClass('is-invalid') || $(returnInputId).hasClass('is-invalid') || $(durationInputId).hasClass('is-invalid')) {
            isFormValid = false;
        }
    }
    return isFormValid;
}


function clearFormValidation(formElement) {
    const form = $(formElement);
    form.removeClass('was-validated');
    form.find('.is-valid, .is-invalid').removeClass('is-valid is-invalid');
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
    // Gasirea detaliilor - ATENTIE: Aceasta metoda este fragila si depinde de ordinea/textul exact!
    // Ar fi mai robust daca elementele P ar avea ID-uri sau atribute data-* unice.
    const detailsItems = card.find('.trip-wrapper-details-item');
    const departureLocation = detailsItems.eq(0).find('p').text().trim();
    const departureDate = detailsItems.eq(1).find('p').text().trim();
    const departureHourRaw = detailsItems.eq(2).find('p').text().trim();
    const departureHour = departureHourRaw.includes(':') ? departureHourRaw.replace(' UTC', '').trim() : '00:00';
    const returnDate = detailsItems.eq(3).find('p').text().trim();
    const availableSpots = detailsItems.eq(4).find('p').text().replace(/\D/g,'').trim();
    const registrationFeeRaw = detailsItems.eq(5).find('p').text().trim();
    const registrationFee = registrationFeeRaw.replace(/[^\d.-]/g, '');
    const hotelName = detailsItems.eq(6).find('p').text().trim();
    let guideElement = detailsItems.eq(7).find('p span'); // Cauta span-ul specific
    let guideEmail = guideElement.length > 0 && guideElement.text().trim() !== 'No guide assigned' ? guideElement.text().trim() : '';

    const description = card.find('.trip-wrapper-description p').text().trim();
    const pictureUrl = card.find('.trip-column-image img').attr('src');


    let initialGuideId = '';
    $('#editGuideSelect option').each(function() {
        if ($(this).text().trim() === guideEmail && guideEmail) {
            initialGuideId = $(this).val();
        }
    });

    $('#editTripId').val(tripId);
    $('#editDestination').val(destination);
    $('#editDepartureLocation').val(departureLocation);
    $('#editDepartureDate').val(departureDate);
    $('#editDepartureHour').val(departureHour);
    $('#editReturnDate').val(returnDate);
    $('#editAvailableSpots').val(availableSpots);
    $('#editRegistrationFee').val(registrationFee);
    $('#editDescription').val(description);
    $('#editHotelName').val(hotelName);
    $('#editPicture').val(pictureUrl);
    $('#editGuideSelect').val(initialGuideId || ''); // Seteaza ghidul selectat

    const today = new Date();
    const tomorrow = new Date(today);
    tomorrow.setDate(today.getDate() + 1);
    const tomorrowString = tomorrow.toISOString().split('T')[0];

    $('#editDepartureDate').attr('min', tomorrowString);

    if (departureDate && departureDate >= tomorrowString) {
        $('#editReturnDate').attr('min', departureDate);
    } else {
        $('#editReturnDate').attr('min', tomorrowString);
    }

    if (departureDate && returnDate) {
        calculateAndUpdateDuration('#editDepartureDate', '#editReturnDate', '#editDurationDays');
    } else {
        $('#editDurationDays').val('').removeClass('is-valid is-invalid');
    }

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
            if (!response.ok) {
                return response.text().then(text => {
                    throw new Error(`HTTP error! Status: ${response.status}, Message: ${text || 'No details provided by server.'}`);
                });
            }
            const contentType = response.headers.get("content-type");
            if (contentType && contentType.indexOf("application/json") !== -1) {
                return response.json();
            } else {
                return [];
            }
        })
        .then(travelers => {
            listElement.innerHTML = '';

            if (travelers && travelers.length > 0) {
                const listGroup = document.createElement('div');
                listGroup.className = 'list-group';

                travelers.forEach(traveler => {
                    const travelerItem = document.createElement('div');
                    travelerItem.className = 'list-group-item d-flex align-items-center';

                    const img = document.createElement('img');
                    img.src = traveler.picture || '/img/default-avatar.png';
                    img.alt = 'Traveler picture';
                    img.width = 65;
                    img.height = 65;
                    img.className = 'rounded-circle mr-2';

                    const spanId = document.createElement('span');
                    spanId.textContent = traveler.user_id || 'Unknown User ID';
                    spanId.className = 'mr-2';

                    const spanName = document.createElement('span');
                    spanName.textContent = `(${traveler.name || traveler.email || ''})`;
                    spanName.style.fontSize = '0.9em';
                    spanName.style.color = '#6c757d';


                    travelerItem.appendChild(img);
                    travelerItem.appendChild(spanId);
                    travelerItem.appendChild(spanName);
                    listGroup.appendChild(travelerItem);
                });
                listElement.appendChild(listGroup);

            } else {
                listElement.innerHTML = '<p>No travelers currently enrolled in this trip.</p>';
            }
        })
        .catch(error => {
            listElement.innerHTML = `<p class="text-danger">Could not load traveler list. Please try again later. (${error.message})</p>`;
        });
}

$(document).ready(function() {

    $('.enroll-button').each(function() {
        const button = $(this);
        const spots = parseInt(button.data('spots'), 10);
        const isGuide = button.data('is-guide') === true || button.data('is-guide') === 'true';
        let popoverContent = '';

        if (isNaN(spots) || spots <= 0) {
            popoverContent = 'Sorry, there are no available spots left.';
        } else if (isGuide) {
            popoverContent = "You can't enroll, you are the guide for this trip.";
        }

        if (popoverContent) {
            button.popover({
                content: popoverContent,
                trigger: 'hover focus',
                placement: 'top',
                container: 'body'
            });
        } else {
            if (button.data('bs.popover')) {
                button.popover('dispose');
            }
        }
    });

    $('.enroll-button').on('click', function(event) {
        const button = $(this);
        const spots = parseInt(button.data('spots'), 10);
        const isGuide = button.data('is-guide') === true || button.data('is-guide') === 'true';
        const tripId = button.data('tripid');
        const destination = button.data('destination');
        let popoverMessage = '';

        if (isNaN(spots) || spots <= 0) {
            popoverMessage = 'Sorry, there are no available spots left.';
        } else if (isGuide) {
            popoverMessage = "You can't enroll, you are the guide for this trip.";
        }

        if (popoverMessage) {
            if (!button.data('bs.popover') || button.data('bs.popover').config.trigger !== 'manual') {
                // Initialize or re-initialize with manual trigger if needed
                if (button.data('bs.popover')) button.popover('dispose'); // Dispose old one first
                button.popover({
                    content: popoverMessage,
                    trigger: 'manual',
                    placement: 'top',
                    container: 'body'
                });
            } else {
                // Update content if popover already exists with manual trigger
                button.data('bs.popover').config.content = popoverMessage;
            }
            button.popover('show');
            setTimeout(function() { button.popover('hide'); }, 2500);
            return; // Stop execution
        }

        // Daca sunt locuri SI user-ul NU e ghid, deschide modalul
        if (button.data('bs.popover')) { // Distruge popover-ul daca exista (cel de hover/focus)
            button.popover('dispose');
        }
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
                if ($(this).data('bs.popover')) {
                    $(this).popover('hide');
                }
            }
        });
    });

    $('#addTripForm').find('input[required], select[required], textarea[required]').on('blur', function() {
        if (this.id !== 'addDurationDays') {
            validateFormField(this);
        }
        if (this.id === 'addDepartureDate' || this.id === 'addReturnDate') {
            const departureDateVal = $('#addDepartureDate').val();
            if (this.id === 'addDepartureDate' && departureDateVal && this.checkValidity()) {
                $('#addReturnDate').attr('min', departureDateVal);
                validateFormField($('#addReturnDate')[0]);
            }
            calculateAndUpdateDuration('#addDepartureDate', '#addReturnDate', '#addDurationDays');
        }
    });

    $('#editTripForm').find('input[required], select[required], textarea[required]').on('blur', function() {
        if (this.id !== 'editDurationDays') {
            validateFormField(this);
        }
        if (this.id === 'editDepartureDate' || this.id === 'editReturnDate') {
            const departureDateVal = $('#editDepartureDate').val();
            if (this.id === 'editDepartureDate' && departureDateVal && this.checkValidity()) {
                $('#editReturnDate').attr('min', departureDateVal);
                validateFormField($('#editReturnDate')[0]);
            }
            calculateAndUpdateDuration('#editDepartureDate', '#editReturnDate', '#editDurationDays');
        }
    });

    $('#enrollForm').find('input[required]').on('blur', function() { validateFormField(this); });

    $('#enrollForm input[name="expirationDate"]').on('input', function(e) {
        var input = $(this), value = input.val(), digits = value.replace(/\D/g, ''), formattedValue = digits;
        if (digits.length > 2) {
            formattedValue = digits.substring(0, 2) + '/' + digits.substring(2, 4);
        } else if (digits.length === 2 && value.length === 3 && value.endsWith('/')) {
            formattedValue = digits;
        }
        input.val(formattedValue);
    });

    $('#addDepartureDate, #addReturnDate').on('input change', function() {
        const departureDateVal = $('#addDepartureDate').val();
        if (this.id === 'addDepartureDate' && departureDateVal && this.checkValidity()) {
            $('#addReturnDate').attr('min', departureDateVal);
            validateFormField($('#addReturnDate')[0]);
        }
        calculateAndUpdateDuration('#addDepartureDate', '#addReturnDate', '#addDurationDays');
    });

    $('#editDepartureDate, #editReturnDate').on('input change', function() {
        const departureDateVal = $('#editDepartureDate').val();
        if (this.id === 'editDepartureDate' && departureDateVal && this.checkValidity()) {
            $('#editReturnDate').attr('min', departureDateVal);
            validateFormField($('#editReturnDate')[0]);
        }
        calculateAndUpdateDuration('#editDepartureDate', '#editReturnDate', '#editDurationDays');
    });

    $('.needs-validation').submit(function(event) {
        const form = this;
        const isValid = validateForm(form);
        if (!isValid) {
            event.preventDefault();
            event.stopPropagation();
            $(form).find('.is-invalid').first().focus();
        }
        $(form).addClass('was-validated');
    });

});