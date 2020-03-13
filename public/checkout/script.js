// Create a Checkout Session with the selected plan ID
var createCheckoutSession = function (name_, email_, introducer_, plan_) {
    return fetch("/create-checkout-session", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            name: name_,
            email: email_,
            introducer: introducer_,
            plan: plan_
        })
    }).catch((e) => {
        alert("リクエスト中にエラーが発生しました。: " + e.message)
    }).then(handleResult);
};

// Handle any errors returned from Checkout
var handleResult = function (result) {
    if (result.error) {
        alert("エラーが発生しました: " + result.error.message);
    } else {
        return result.json();
    }
};

function checkEmailAddress(str) {
    if (str.match(/.+@.+\..+/) == null) {
        return false;
    } else {
        return true;
    }
}

fetch("https://script.google.com/macros/s/AKfycbyMvZkqDMBzLNiRqHiBG71Hr-qI3eVYn4qO3KM1Hg/exec?action=introducer")
    .then(function (result) {
        return result.json();
    })
    .then(function (json) {
        var select = document.getElementById('introducer');
        json.introducer.map(function (v) {
            var option = document.createElement('option');
            option.value = v;
            option.appendChild(document.createTextNode(v));
            select.appendChild(option);
        });
    });

/* Get your Stripe publishable key to initialize Stripe.js */
fetch("/setup")
    .then(function (result) {
        return result.json();
    })
    .then(function (json) {
        var publicKey = json.publicKey;
        var stripe = Stripe(publicKey);
        var plan1 = json.plan1;
        var plan2 = json.plan2;

        // Setup event handler to create a Checkout Session when button is clicked
        document
            .getElementById("checkout-button-1")
            .addEventListener("click", function (evt) {
                var name = document.getElementById("name-input").value
                if (name == undefined || name == "") {
                    alert("名前を入力してください。");
                    return;
                }
                var email = document.getElementById("email-input").value
                if (email == undefined || email == "") {
                    alert("メールアドレスを入力してください。");
                    return;
                }
                if (!checkEmailAddress(email)) {
                    alert("正しいメールアドレスを入力してください。")
                    return;
                }
                var introducer = document.getElementById("introducer").value
                if (introducer == undefined || introducer == "ご紹介者様を選択してください") {
                    alert("ご紹介者様を選択してください。");
                    return;
                }
                createCheckoutSession(name, email, introducer, plan1).then(function (data) {
                    // Call Stripe.js method to redirect to the new Checkout page
                    stripe
                        .redirectToCheckout({
                            sessionId: data.sessionId
                        })
                        .then(handleResult);
                });
            });

    });
