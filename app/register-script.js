const form = document.getElementById("login-form"); 
const usernameInput = document.querySelector("#username");
const passwordInput = document.querySelector("#password");
const firstnameInput = document.querySelector("#first-name");
const lastnameInput = document.querySelector("#last-name");
const dobInput = document.querySelector("#dob");
const message = document.querySelector(".message"); 

function showMessage(text, kind) {
  message.textContent = text;
  message.classList.remove("hidden", "error", "success");
  message.classList.add(kind);
}

function markInvalid(input, isInvalid) {
  input.classList.toggle("invalid", isInvalid);
}


form.addEventListener("submit", (event) => {
  event.preventDefault();
  const usernameOk = usernameInput.value.length >= 3;
  const passwordOk = passwordInput.value.length >= 8;
  const firstnameOk = firstnameInput.value.length >= 2;
  const lastnameOk = passwordInput.value.length >= 2;
//   const passwordOk = passwordInput.value.length >= 8;

  markInvalid(usernameInput, !usernameOk); 
  markInvalid(passwordInput, !passwordOk);
  markInvalid(firstnameOk, !firstnameOk); 
  markInvalid(lastnameOk, !lastnameOk);

  if (!usernameOk || !passwordOk || !firstnameOk || !lastnameOk) {
    showMessage("Username needs 3+ chars, " + "password needs 8+ ", "error");
    return;
  }
  showMessage("Successfully submitted", "success")
  form.reset()
})


usernameInput.addEventListener("input", () => {
  if (usernameInput.value.length >= 3) {
    markInvalid(usernameInput, false);
  }
});