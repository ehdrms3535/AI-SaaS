const API_BASE = window.location.origin;

const el = {
  form: document.getElementById("resetPasswordForm"),
  newPassword: document.getElementById("resetPasswordNew"),
  confirmPassword: document.getElementById("resetPasswordConfirm"),
  feedback: document.getElementById("resetPasswordFeedback"),
  submitBtn: document.getElementById("resetPasswordSubmitBtn"),
};

const token = new URLSearchParams(window.location.search).get("token") || "";

function setFeedback(message = "", tone = "") {
  el.feedback.textContent = message;
  el.feedback.classList.remove("success", "error");
  if (tone) {
    el.feedback.classList.add(tone);
  }
}

function setPending(pending, label) {
  if (!el.submitBtn.dataset.defaultLabel) {
    el.submitBtn.dataset.defaultLabel = el.submitBtn.textContent;
  }
  el.submitBtn.disabled = pending;
  el.submitBtn.textContent = pending ? label : el.submitBtn.dataset.defaultLabel;
}

async function api(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {}),
    },
    ...options,
  });
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) {
    throw data || { message: "요청에 실패했습니다." };
  }
  return data;
}

function wirePasswordToggles() {
  document.querySelectorAll("[data-password-toggle]").forEach((button) => {
    button.addEventListener("click", () => {
      const field = button.closest(".password-field")?.querySelector("input");
      if (!field) {
        return;
      }
      const nextType = field.type === "password" ? "text" : "password";
      field.type = nextType;
      button.textContent = nextType === "password" ? "보기" : "숨기기";
    });
  });
}

async function validateToken() {
  if (!token) {
    setFeedback("재설정 토큰이 없습니다. 로그인 화면에서 다시 요청해 주세요.", "error");
    el.submitBtn.disabled = true;
    return;
  }
  try {
    await api(`/api/auth/password-reset/validate?token=${encodeURIComponent(token)}`);
    el.newPassword.focus();
  } catch (error) {
    setFeedback(error.message || "재설정 링크가 유효하지 않습니다.", "error");
    el.submitBtn.disabled = true;
  }
}

el.form.addEventListener("submit", async (event) => {
  event.preventDefault();
  setFeedback();

  const newPassword = el.newPassword.value;
  const confirmPassword = el.confirmPassword.value;

  if (!newPassword || !confirmPassword) {
    setFeedback("새 비밀번호를 모두 입력해 주세요.", "error");
    return;
  }
  if (newPassword !== confirmPassword) {
    setFeedback("새 비밀번호와 확인 값이 일치하지 않습니다.", "error");
    el.confirmPassword.focus();
    return;
  }

  setPending(true, "저장 중...");
  try {
    await api("/api/auth/password-reset/confirm", {
      method: "POST",
      body: JSON.stringify({ token, newPassword }),
    });
    setFeedback("비밀번호가 변경되었습니다. 로그인 화면으로 이동해 주세요.", "success");
    window.setTimeout(() => {
      window.location.href = "/";
    }, 900);
  } catch (error) {
    setFeedback(error.message || "비밀번호 변경에 실패했습니다.", "error");
  } finally {
    setPending(false);
  }
});

wirePasswordToggles();
validateToken();
