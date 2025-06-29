document.addEventListener('DOMContentLoaded', () => {

    const passwordInput = document.getElementById('password') || document.getElementById('newPassword');

    const lengthRule = document.getElementById('length');
    const numberRule = document.getElementById('number');
    const specialRule = document.getElementById('special');

    // Only run the script if all necessary elements are found on the page
    if (passwordInput && lengthRule && numberRule && specialRule) {

        const validate = () => {
            const password = passwordInput.value;

            // Rule 1: Length
            if (password.length >= 8) {
                lengthRule.classList.replace('invalid', 'valid');
            } else {
                lengthRule.classList.replace('valid', 'invalid');
            }

            // Rule 2: Number
            if (/[0-9]/.test(password)) {
                numberRule.classList.replace('invalid', 'valid');
            } else {
                numberRule.classList.replace('valid', 'invalid');
            }

            // Rule 3: Special character
            if (/[!@#$%^&*]/.test(password)) {
                specialRule.classList.replace('invalid', 'valid');
            } else {
                specialRule.classList.replace('valid', 'invalid');
            }
        };

        passwordInput.addEventListener('keyup', validate);
    }
});