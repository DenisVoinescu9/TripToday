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
        calculateAndUpdateDuration(departureInputId, returnInputId, durationInputId);
        if (form.find(departureInputId).hasClass('is-invalid') || form.find(returnInputId).hasClass('is-invalid') || form.find(durationInputId).hasClass('is-invalid')) {
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
    clearFormValidation(form);

    const tripId = $(button).data('tripid');
    const row = $(button).closest('tr');

    const destination = row.find('td:eq(0)').text().trim();
    const departureLocation = row.find('td:eq(1)').text().trim();
    const departureDate = row.find('td:eq(2)').text().trim();
    const departureHourRaw = row.find('td:eq(3)').text().trim();
    const departureHour = departureHourRaw.replace(' UTC', '').trim();
    const returnDate = row.find('td:eq(4)').text().trim();
    const availableSpots = row.find('td:eq(6)').text().trim();
    const registrationFeeRaw = row.find('td:eq(7)').text().trim();
    const registrationFee = registrationFeeRaw.replace(' RON', '').trim();
    let guideEmail = row.find('td:eq(8) span:first').text().trim();
    let initialGuideId = '';
    $('#editGuideSelect option').each(function() {
        if ($(this).text().trim() === guideEmail && guideEmail !== 'No guide assigned') {
            initialGuideId = $(this).val();
        }
    });

    const description = row.find('td:eq(9)').text().trim();
    const hotelName = row.find('td:eq(10)').text().trim();
    const pictureUrl = row.find('td:eq(11) img').attr('src');

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
    if (departureDate >= tomorrowString) {
        $('#editReturnDate').attr('min', departureDate);
    } else {
        $('#editReturnDate').attr('min', tomorrowString);
    }

    if (departureDate && returnDate) {
        calculateAndUpdateDuration('#editDepartureDate', '#editReturnDate', '#editDurationDays');
    } else {
        $('#editDurationDays').val('').removeClass('is-valid is-invalid');
    }
    validateForm('#editTripForm');
    form.addClass('was-validated');

    $('#editTripModal').modal('show');
}

function openDeleteModal(button) {
    const tripId = $(button).data('tripid');
    $('#deleteTripId').val(tripId);
    $('#deleteTripModal').modal('show');
}

$(document).ready(function() {

    $('.enroll-button').each(function() {
        const button = $(this);
        const spots = parseInt(button.data('spots'), 10);
        if (isNaN(spots) || spots <= 0) {
            button.popover({
                content: 'Sorry, there are no available spots left.',
                trigger: 'focus',
                placement: 'top',
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
        const tripId = button.data('tripid');
        const destination = button.data('destination');

        if (isNaN(spots) || spots <= 0) {
            event.preventDefault();
        } else {
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
            if (!$(this).is(e.target) && $(this).has(e.target).length === 0 && $('.popover').has(e.target).length === 0) {
                if ($(this).data('bs.popover')) {
                    $(this).popover('hide');
                }
            }
        });
    });

    $('#addTripForm').find('input[required], select[required], textarea[required]').on('blur', function() {
        if (this.id !== 'addDurationDays') validateFormField(this);
        if (this.id === 'addDepartureDate' || this.id === 'addReturnDate') {
            const departureDateVal = $('#addDepartureDate').val();
            if (this.id === 'addDepartureDate' && departureDateVal && this.checkValidity()) $('#addReturnDate').attr('min', departureDateVal);
            calculateAndUpdateDuration('#addDepartureDate', '#addReturnDate', '#addDurationDays');
            const otherDateFieldId = (this.id === 'addDepartureDate') ? '#addReturnDate' : '#addDepartureDate';
            if ($(otherDateFieldId).val()) validateFormField($(otherDateFieldId)[0]);
        }
    });

    $('#editTripForm').find('input[required], select[required], textarea[required]').on('blur', function() {
        if (this.id !== 'editDurationDays') validateFormField(this);
        if (this.id === 'editDepartureDate' || this.id === 'editReturnDate') {
            const departureDateVal = $('#editDepartureDate').val();
            if (this.id === 'editDepartureDate' && departureDateVal && this.checkValidity()) $('#editReturnDate').attr('min', departureDateVal);
            calculateAndUpdateDuration('#editDepartureDate', '#editReturnDate', '#editDurationDays');
            const otherDateFieldId = (this.id === 'editDepartureDate') ? '#editReturnDate' : '#editDepartureDate';
            if ($(otherDateFieldId).val()) validateFormField($(otherDateFieldId)[0]);
        }
    });

    $('#enrollForm').find('input[required]').on('blur', function() { validateFormField(this); });

    $('#enrollForm input[name="expirationDate"]').on('input', function(e) {
        var input = $(this), value = input.val(), digits = value.replace(/\D/g, ''), formattedValue = digits;
        if (digits.length >= 2) formattedValue = digits.substring(0, 2) + '/' + digits.substring(2, 4);
        input.val(formattedValue);
    });

    $('#addDepartureDate, #addReturnDate').on('input change', function() {
        const departureDateVal = $('#addDepartureDate').val();
        if (this.id === 'addDepartureDate' && departureDateVal && this.checkValidity()) $('#addReturnDate').attr('min', departureDateVal);
        calculateAndUpdateDuration('#addDepartureDate', '#addReturnDate', '#addDurationDays');
    });

    $('#editDepartureDate, #editReturnDate').on('input change', function() {
        const departureDateVal = $('#editDepartureDate').val();
        if (this.id === 'editDepartureDate' && departureDateVal && this.checkValidity()) $('#editReturnDate').attr('min', departureDateVal);
        calculateAndUpdateDuration('#editDepartureDate', '#editReturnDate', '#editDurationDays');
    });

    $('.needs-validation').submit(function(event) {
        const form = this;
        const isValid = validateForm(form);
        if (!isValid) {
            event.preventDefault();
            event.stopPropagation();
        }
        $(form).addClass('was-validated');
    });

});



