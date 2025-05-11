$(document).ready(function () {
    const descriptionTextarea = $('#description');
    const charCountDisplay = $('#description-char-count');

    if (descriptionTextarea.length > 0) {
        const maxLength = parseInt(descriptionTextarea.attr('maxlength'), 10);

        function updateCounter() {
            if (charCountDisplay.length > 0 && !isNaN(maxLength)) {
                const currentLength = descriptionTextarea.val().length;
                charCountDisplay.text(currentLength + '/' + maxLength);

                if (currentLength > maxLength * 0.95) {
                    charCountDisplay.removeClass('text-danger').addClass('text-warning');
                } else {
                    charCountDisplay.removeClass('text-danger text-warning');
                }
            }
        }

        updateCounter();
        descriptionTextarea.on('input', updateCounter);
    }

    const upcomingBtn = $('#show-upcoming-btn');
    const pastBtn = $('#show-past-btn');
    const upcomingContent = $('#upcoming-trips-content');
    const pastContent = $('#past-trips-content');

    upcomingBtn.on('click', function () {
        if (!$(this).hasClass('active')) {
            pastContent.hide();
            upcomingContent.show();
            $(this).addClass('active');
            pastBtn.removeClass('active');
        }
    });

    pastBtn.on('click', function () {
        if (!$(this).hasClass('active')) {
            upcomingContent.hide();
            pastContent.show();
            $(this).addClass('active');
            upcomingBtn.removeClass('active');
        }
    });

});