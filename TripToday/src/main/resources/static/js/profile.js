// Wait for DOM ready
$(document).ready(function() {
    // Description character counter
    const descriptionTextarea = $('#description');
    const charCountDisplay = $('#description-char-count');

    // If description textarea exists
    if (descriptionTextarea.length > 0) {
        // Get max length from attribute
        const maxLength = parseInt(descriptionTextarea.attr('maxlength'), 10);

        function updateCounter() {
            // If display element and maxLength are valid
            if (charCountDisplay.length > 0 && !isNaN(maxLength)) {
                const currentLength = descriptionTextarea.val().length;
                // Update counter text display
                charCountDisplay.text(currentLength + '/' + maxLength);

                // Change text color near limit
                if (currentLength > maxLength * 0.95) { // 95% of max
                    charCountDisplay.removeClass('text-danger').addClass('text-warning'); // Set warning color
                } else {
                    // Reset text color
                    charCountDisplay.removeClass('text-danger text-warning');
                }
            }
        }
        updateCounter(); // Initial counter set
        descriptionTextarea.on('input', updateCounter); // Bind update to input
    }

    // Trip tabs toggle logic
    const upcomingBtn = $('#show-upcoming-btn');
    const pastBtn = $('#show-past-btn');
    const upcomingContent = $('#upcoming-trips-content');
    const pastContent = $('#past-trips-content');

    // Handle click on upcoming trips button
    upcomingBtn.on('click', function() {
        // Only if not already active
        if (!$(this).hasClass('active')) {
            pastContent.hide(); // Hide past trips section
            upcomingContent.show(); // Show upcoming trips section
            $(this).addClass('active'); // Set this button active
            pastBtn.removeClass('active'); // Make other button inactive
        }
    });

    // Handle click on past trips button
    pastBtn.on('click', function() {
        // Only if not already active
        if (!$(this).hasClass('active')) {
            upcomingContent.hide(); // Hide upcoming trips section
            pastContent.show(); // Show past trips section
            $(this).addClass('active'); // Set this button active
            upcomingBtn.removeClass('active'); // Make other button inactive
        }
    });

});