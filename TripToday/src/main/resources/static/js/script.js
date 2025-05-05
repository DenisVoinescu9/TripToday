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
    // --- Modificare: Gaseste cardul parinte in loc de randul de tabel ---
    const card = $(button).closest('.trip-card');

    // --- Modificare: Extrage datele din elementele specifice ale cardului ---
    // Gaseste elementele relevante in interiorul cardului, folosind clase sau structura DOM
    // Nota: Aceste selectoare sunt exemple si trebuie adaptate EXACT la structura HTML finala!
    // Presupunand ca folosim clase/id-uri sau data-atribute pe elementele H1/P din .trip-wrapper-details-item

    // Exemplu ipotetic (necesita adaptare la HTML-ul tau exact!)
    const destination = card.find('.trip-column-image p').text().trim(); // Destinatia din coloana imaginii
    const departureLocation = card.find('.trip-wrapper-details-item:contains("Departure location") p').text().trim(); // Gaseste dupa textul H1 - fragil
    const departureDate = card.find('.trip-wrapper-details-item:contains("Departure date") p').text().trim(); // Gaseste dupa textul H1 - fragil
    const departureHourRaw = card.find('.trip-wrapper-details-item:contains("Departure hour") p').text().trim(); // Gaseste dupa textul H1 - fragil
    const departureHour = departureHourRaw.includes(':') ? departureHourRaw.replace(' UTC', '').trim() : '00:00';
    const returnDate = card.find('.trip-wrapper-details-item:contains("Return date") p').text().trim(); // Gaseste dupa textul H1 - fragil
    const availableSpots = card.find('.trip-wrapper-details-item:contains("Remaining spots") p').text().replace(/\D/g,'').trim(); // Ia doar cifrele
    const registrationFeeRaw = card.find('.trip-wrapper-details-item:contains("Enrollment fee") p').text().trim(); // Gaseste dupa textul H1 - fragil
    const registrationFee = registrationFeeRaw.replace(/[^\d.-]/g, '');
    let guideElement = card.find('.trip-guide-item p span'); // Gaseste span-ul din item-ul de ghid
    let guideEmail = guideElement.length > 0 ? guideElement.text().trim() : 'No guide assigned';
    const description = card.find('.trip-wrapper-description p').text().trim(); // Descrierea
    const hotelName = card.find('.trip-wrapper-details-item:contains("Hotel") p').text().trim(); // Gaseste dupa textul H1 - fragil
    const pictureUrl = card.find('.trip-column-image img').attr('src'); // URL-ul imaginii


    let initialGuideId = '';
    $('#editGuideSelect option').each(function() {
        if ($(this).text().trim() === guideEmail && guideEmail !== 'No guide assigned') {
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
    $('#editGuideSelect').val(initialGuideId || '');

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
        // Initializam popover DOAR daca locurile sunt 0 pentru a arata mesajul la hover/focus
        if (isNaN(spots) || spots <= 0) {
            button.popover({
                content: 'Sorry, there are no available spots left.',
                trigger: 'hover focus', // Arata la hover/focus initial
                placement: 'top',
                container: 'body'
            });
        } else {
            // Daca sunt locuri, ne asiguram ca nu exista un popover ramas de la o stare anterioara
            if (button.data('bs.popover')) {
                button.popover('dispose');
            }
        }
    });

    $('.enroll-button').on('click', function(event) {
        const button = $(this);
        const spots = parseInt(button.data('spots'), 10);
        const tripId = button.data('tripid');
        const destination = button.data('destination');

        // Verifica locurile INAINTE de a deschide modalul
        if (isNaN(spots) || spots <= 0) {
            // Optional: Arata popover scurt pentru feedback la click pe buton dezactivat/plin
            if (!button.data('bs.popover')) {
                button.popover({
                    content: 'Sorry, there are no available spots left.',
                    trigger: 'manual',
                    placement: 'top',
                    container: 'body'
                });
            }
            button.popover('show');
            setTimeout(function() { button.popover('hide'); }, 2500);

            // Opreste executia ulterioara, NU deschide modalul
            return;
        } else {
            // Daca sunt locuri, distruge popover-ul (daca exista) si deschide modalul
            if (button.data('bs.popover')) {
                button.popover('dispose');
            }
            const enrollModal = $('#enrollModal');
            const enrollForm = $('#enrollForm');
            clearFormValidation(enrollForm);
            enrollForm[0].reset();
            enrollModal.find('#modalTripId').val(tripId);
            enrollModal.find('#modalDestination').text(destination);
            enrollModal.modal('show');
        }
    });

    $('body').on('click', function (e) {
        $('.enroll-button').each(function () {
            // Daca se da click in afara butonului SI in afara popover-ului deschis
            if (!$(this).is(e.target) && $(this).has(e.target).length === 0 && $('.popover').has(e.target).length === 0) {
                // Ascunde popover-ul daca exista (util pt cele manuale sau hover/focus)
                if ($(this).data('bs.popover')) {
                    // Verifica daca popover-ul este vizibil inainte de a incerca sa il ascunzi
                    // (Metoda isVisible poate depinde de versiunea exacta de Bootstrap/jQuery)
                    // O abordare mai sigura e sa incerci sa-l ascunzi oricum.
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