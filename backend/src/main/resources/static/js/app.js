const state = {
    token: sessionStorage.getItem("access") || "",
    user: null,
    page: 0,
    totalPages: 1,
    selected: null,
    passengers: []
};

const message = document.getElementById("message");

function show(text, ok) {
    message.textContent = text || "";
    message.className = ok ? "message ok" : "message";
}

function roles() {
    return state.user && state.user.roles ? state.user.roles : [];
}

function canEdit() {
    return roles().includes("EDITOR") || roles().includes("ADMIN");
}

function isAdmin() {
    return roles().includes("ADMIN");
}

async function api(path, options = {}) {
    const headers = Object.assign({}, options.headers || {});
    const isForm = options.body instanceof FormData;
    if (!isForm && options.body) {
        headers["Content-Type"] = "application/json";
    }
    if (state.token) {
        headers.Authorization = "Bearer " + state.token;
    }
    let response = await fetch(path, Object.assign({}, options, { headers, credentials: "include" }));
    if (response.status === 401 && path !== "/api/auth/login" && path !== "/api/auth/refresh") {
        const refreshed = await fetch("/api/auth/refresh", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: "{}",
            credentials: "include"
        });
        if (refreshed.ok) {
            const data = await refreshed.json();
            state.token = data.accessToken;
            sessionStorage.setItem("access", data.accessToken);
            headers.Authorization = "Bearer " + state.token;
            response = await fetch(path, Object.assign({}, options, { headers, credentials: "include" }));
        }
    }
    if (response.status === 204) {
        return null;
    }
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
        const error = new Error(data.message || "Запрос не выполнен");
        error.code = data.code;
        throw error;
    }
    return data;
}

function applyRoleVisibility() {
    document.querySelectorAll(".editor-only").forEach(node => node.classList.toggle("denied", !canEdit()));
    document.querySelectorAll(".admin-only").forEach(node => node.classList.toggle("denied", !isAdmin()));
}

function showApp() {
    document.getElementById("login-panel").classList.add("hidden");
    document.getElementById("password-panel").classList.toggle("hidden", !state.user.mustChangePassword);
    document.getElementById("app").classList.toggle("hidden", state.user.mustChangePassword);
    document.getElementById("who").textContent = state.user.name + " · " + state.user.email + " · " + roles().join(", ");
    applyRoleVisibility();
    if (!state.user.mustChangePassword) {
        loadBookings();
    }
}

async function login() {
    const data = await api("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({
            email: document.getElementById("login-email").value,
            password: document.getElementById("login-password").value
        })
    });
    state.token = data.accessToken;
    sessionStorage.setItem("access", data.accessToken);
    state.user = data.user;
    show(data.mustChangePassword ? "Смените временный пароль." : "Вход выполнен.", true);
    showApp();
}

function queryString() {
    const params = new URLSearchParams();
    const q = document.getElementById("q").value;
    const status = document.getElementById("status").value;
    const priority = document.getElementById("priority").value;
    const refundable = document.getElementById("refundable").value;
    const tag = document.getElementById("tag").value;
    const priceOp = document.getElementById("price-op").value;
    const price = document.getElementById("price").value;
    if (q) params.set("q", q);
    if (status) params.set("status", status);
    if (priority) params.set("priority", priority);
    if (refundable) params.set("refundable", refundable);
    if (tag) params.set("tag", tag);
    params.set("view", document.getElementById("view").value);
    if (priceOp && price) {
        params.set("priceOp", priceOp);
        params.set("price", price);
    }
    params.set("sort", document.getElementById("sort").value);
    params.set("direction", document.getElementById("direction").value);
    params.set("page", state.page);
    params.set("size", "10");
    return params;
}

async function loadPassengers() {
    state.passengers = await api("/api/passengers");
    const select = document.getElementById("form-passenger");
    select.innerHTML = "";
    state.passengers.forEach(item => {
        const option = document.createElement("option");
        option.value = item.id;
        option.textContent = item.lastName + " " + item.firstName + (item.active ? "" : " (отключен)");
        select.appendChild(option);
    });
    const body = document.querySelector("#passenger-table tbody");
    body.innerHTML = "";
    state.passengers.forEach(item => {
        const row = document.createElement("tr");
        row.innerHTML = "<td></td><td></td><td></td><td></td><td></td><td></td>";
        row.children[0].textContent = item.lastName + " " + item.firstName;
        row.children[1].textContent = item.email;
        row.children[2].textContent = item.phone;
        row.children[3].textContent = item.passportNumber;
        row.children[4].textContent = item.active ? "да" : "нет";
        if (isAdmin()) {
            const button = document.createElement("button");
            button.type = "button";
            button.textContent = item.active ? "Отключить" : "Включить";
            button.addEventListener("click", async () => {
                await api("/api/passengers/" + item.id + "/active", {
                    method: "POST",
                    body: JSON.stringify({ active: !item.active })
                });
                await loadPassengers();
            });
            row.children[5].appendChild(button);
        }
        body.appendChild(row);
    });
}

async function loadBookings() {
    await loadPassengers();
    const data = await api("/api/bookings?" + queryString().toString());
    state.totalPages = data.totalPages || 1;
    document.getElementById("page-label").textContent = "Страница " + (data.page + 1) + " из " + Math.max(data.totalPages, 1) + ", всего " + data.totalElements;
    const body = document.querySelector("#booking-table tbody");
    body.innerHTML = "";
    data.content.forEach(item => {
        const row = document.createElement("tr");
        if (state.selected && state.selected.id === item.id) {
            row.className = "selected";
        }
        const check = document.createElement("input");
        check.type = "checkbox";
        check.value = item.id;
        const cell = document.createElement("td");
        cell.appendChild(check);
        row.appendChild(cell);
        [item.bookingCode, item.passengerName, item.flightNumber, item.departureCity + " → " + item.arrivalCity,
            item.departureDate.replace("T", " "), item.price, item.status, item.priority, item.tags.join(", ")
        ].forEach(value => {
            const td = document.createElement("td");
            td.textContent = value;
            row.appendChild(td);
        });
        row.addEventListener("click", event => {
            if (event.target.type === "checkbox") return;
            state.selected = item;
            loadNotes(item.id);
            fillForm(item);
        });
        body.appendChild(row);
    });
}

function selectedIds() {
    return Array.from(document.querySelectorAll("#booking-table tbody input:checked")).map(node => node.value);
}

function fillForm(item) {
    document.getElementById("booking-form").classList.remove("hidden");
    document.getElementById("form-title").textContent = "Изменение " + item.bookingCode;
    document.getElementById("form-passenger").value = item.passengerId;
    document.getElementById("form-flight").value = item.flightNumber;
    document.getElementById("form-from").value = item.departureCity;
    document.getElementById("form-to").value = item.arrivalCity;
    document.getElementById("form-date").value = item.departureDate.slice(0, 16);
    document.getElementById("form-price").value = item.price;
    document.getElementById("form-refundable").checked = item.refundable;
    document.getElementById("form-tags").value = item.tags.join(", ");
    document.getElementById("form-status").value = item.status;
    document.getElementById("status-row").classList.remove("hidden");
}

function bookingBody(editing) {
    const body = {
        passengerId: document.getElementById("form-passenger").value,
        flightNumber: document.getElementById("form-flight").value,
        departureCity: document.getElementById("form-from").value,
        arrivalCity: document.getElementById("form-to").value,
        departureDate: document.getElementById("form-date").value + ":00",
        price: document.getElementById("form-price").value,
        refundable: document.getElementById("form-refundable").checked,
        tags: document.getElementById("form-tags").value.split(",").map(tag => tag.trim()).filter(Boolean)
    };
    if (editing) {
        body.status = document.getElementById("form-status").value;
        body.version = state.selected.version;
    }
    return body;
}

async function loadNotes(id) {
    const notes = await api("/api/bookings/" + id + "/notes");
    document.getElementById("notes").classList.remove("hidden");
    const list = document.getElementById("note-list");
    list.innerHTML = "";
    notes.forEach(note => {
        const item = document.createElement("li");
        item.textContent = (note.pinned ? "[закреплено] " : "") + note.text + " — " + note.authorName;
        if (canEdit()) {
            const button = document.createElement("button");
            button.type = "button";
            button.textContent = "Удалить";
            button.addEventListener("click", async () => {
                await api("/api/bookings/" + id + "/notes/" + note.id, { method: "DELETE" });
                loadNotes(id);
            });
            item.appendChild(button);
        }
        list.appendChild(item);
    });
}

async function loadAnalytics() {
    const data = await api("/api/analytics/summary");
    const groups = (title, rows) => "<h3>" + title + "</h3><ul>" + rows.map(row => "<li>" + row.name + ": " + row.count + "</li>").join("") + "</ul>";
    document.getElementById("analytics").innerHTML =
        "<p>Всего: " + data.total + ", активных: " + data.active + ", в архиве: " + data.archived +
        ", возвратных: " + data.refundable + ", средняя цена: " + data.averagePrice + "</p>" +
        groups("По пассажирам", data.byPassenger) + groups("По статусам", data.byStatus) + groups("По приоритету", data.byPriority);
}

async function loadUsers() {
    const data = await api("/api/admin/users?size=50&sort=email&direction=asc");
    const body = document.querySelector("#user-table tbody");
    body.innerHTML = "";
    data.content.forEach(user => {
        const row = document.createElement("tr");
        row.innerHTML = "<td></td><td></td><td></td><td></td><td></td>";
        row.children[0].textContent = user.email;
        row.children[1].textContent = user.name;
        row.children[2].textContent = user.roles.join(", ");
        row.children[3].textContent = user.active ? "да" : "нет";
        const button = document.createElement("button");
        button.type = "button";
        button.textContent = "Сбросить пароль";
        button.addEventListener("click", async () => {
            const result = await api("/api/admin/users/" + user.id + "/reset-password", { method: "POST" });
            show("Временный пароль для " + user.email + ": " + result.temporaryPassword, true);
        });
        row.children[4].appendChild(button);
        body.appendChild(row);
    });
}

async function loadAudit() {
    const q = document.getElementById("audit-q").value;
    const data = await api("/api/admin/audit?size=30&sort=createdAt&direction=desc" + (q ? "&q=" + encodeURIComponent(q) : ""));
    const body = document.querySelector("#audit-table tbody");
    body.innerHTML = "";
    data.content.forEach(event => {
        const row = document.createElement("tr");
        [event.createdAt.replace("T", " "), event.eventType, event.userEmail || "—", event.description, event.correlationId || ""].forEach(value => {
            const td = document.createElement("td");
            td.textContent = value;
            row.appendChild(td);
        });
        body.appendChild(row);
    });
}

document.getElementById("login-button").addEventListener("click", () => login().catch(error => show(error.message)));
document.getElementById("password-button").addEventListener("click", async () => {
    try {
        await api("/api/auth/password", {
            method: "POST",
            body: JSON.stringify({
                currentPassword: document.getElementById("old-password").value,
                newPassword: document.getElementById("new-password").value
            })
        });
        state.user.mustChangePassword = false;
        show("Пароль изменен. Войдите снова, если сессия сбросилась.", true);
        showApp();
    } catch (error) {
        show(error.message);
    }
});
document.getElementById("logout-button").addEventListener("click", async () => {
    await api("/api/auth/logout", { method: "POST", body: "{}" }).catch(() => null);
    sessionStorage.removeItem("access");
    state.token = "";
    location.reload();
});
document.querySelectorAll("nav button[data-tab]").forEach(button => {
    button.addEventListener("click", () => {
        document.querySelectorAll("[id^='tab-']").forEach(tab => tab.classList.add("hidden"));
        document.getElementById("tab-" + button.dataset.tab).classList.remove("hidden");
        if (button.dataset.tab === "analytics") loadAnalytics().catch(error => show(error.message));
        if (button.dataset.tab === "users") loadUsers().catch(error => show(error.message));
        if (button.dataset.tab === "audit") loadAudit().catch(error => show(error.message));
        if (button.dataset.tab === "passengers") loadPassengers().catch(error => show(error.message));
    });
});
document.getElementById("search-button").addEventListener("click", () => {
    state.page = 0;
    loadBookings().catch(error => show(error.message));
});
document.getElementById("prev-page").addEventListener("click", () => {
    if (state.page > 0) {
        state.page--;
        loadBookings().catch(error => show(error.message));
    }
});
document.getElementById("next-page").addEventListener("click", () => {
    if (state.page + 1 < state.totalPages) {
        state.page++;
        loadBookings().catch(error => show(error.message));
    }
});
document.getElementById("new-booking").addEventListener("click", () => {
    state.selected = null;
    document.getElementById("booking-form").classList.remove("hidden");
    document.getElementById("form-title").textContent = "Новое бронирование";
    document.getElementById("booking-form").reset();
    document.getElementById("status-row").classList.add("hidden");
});
document.getElementById("booking-form").addEventListener("submit", async event => {
    event.preventDefault();
    try {
        if (state.selected) {
            await api("/api/bookings/" + state.selected.id, { method: "PUT", body: JSON.stringify(bookingBody(true)) });
        } else {
            await api("/api/bookings", { method: "POST", body: JSON.stringify(bookingBody(false)) });
        }
        show("Сохранено.", true);
        await loadBookings();
    } catch (error) {
        show(error.message);
    }
});
document.getElementById("archive-one").addEventListener("click", async () => {
    if (!state.selected) return show("Сначала выберите строку.");
    await api("/api/bookings/" + state.selected.id, { method: "DELETE" });
    show("Отправлено в архив.", true);
    loadBookings();
});
document.getElementById("restore-one").addEventListener("click", async () => {
    if (!state.selected) return show("Сначала выберите строку.");
    await api("/api/bookings/" + state.selected.id + "/restore", { method: "POST" });
    show("Восстановлено.", true);
    loadBookings();
});
document.getElementById("delete-one").addEventListener("click", async () => {
    if (!state.selected) return show("Сначала выберите строку.");
    await api("/api/bookings/" + state.selected.id + "/permanent", { method: "DELETE" });
    show("Удалено.", true);
    loadBookings();
});
document.getElementById("bulk-status").addEventListener("click", async () => {
    const status = prompt("Новый статус: CREATED, CONFIRMED, COMPLETED или CANCELLED");
    if (!status) return;
    const result = await api("/api/bookings/bulk", {
        method: "POST",
        body: JSON.stringify({ action: "SET_STATUS", ids: selectedIds(), status })
    });
    show("Успешно: " + result.succeeded.length + ", ошибок: " + result.failed.length, result.failed.length === 0);
    loadBookings();
});
document.getElementById("bulk-tag").addEventListener("click", () => bulkTag("ADD_TAG"));
document.getElementById("bulk-untag").addEventListener("click", () => bulkTag("REMOVE_TAG"));

async function bulkTag(action) {
    const tag = prompt("Тег");
    if (!tag) return;
    const result = await api("/api/bookings/bulk", {
        method: "POST",
        body: JSON.stringify({ action, ids: selectedIds(), tag })
    });
    show("Успешно: " + result.succeeded.length + ", ошибок: " + result.failed.length, true);
    loadBookings();
}

document.getElementById("export-button").addEventListener("click", () => {
    const params = queryString();
    params.delete("page");
    params.delete("size");
    fetch("/api/bookings/export?" + params.toString(), { headers: { Authorization: "Bearer " + state.token } })
        .then(response => response.blob())
        .then(blob => {
            const link = document.createElement("a");
            link.href = URL.createObjectURL(blob);
            link.download = "bookings.csv";
            link.click();
        });
});

async function sendCsv(path) {
    const file = document.getElementById("csv-file").files[0];
    if (!file) throw new Error("Выберите CSV-файл.");
    const body = new FormData();
    body.append("file", file);
    const mode = document.getElementById("csv-mode").value;
    return api(path + "?mode=" + mode, { method: "POST", body });
}

document.getElementById("preview-button").addEventListener("click", () => {
    sendCsv("/api/bookings/import/preview")
        .then(data => {
            document.getElementById("import-result").textContent =
                "Корректных: " + data.validCount + ", с ошибками: " + data.errorCount + "\n" +
                data.errors.map(error => "Строка " + error.row + ": " + error.message).join("\n");
        })
        .catch(error => show(error.message));
});
document.getElementById("apply-button").addEventListener("click", () => {
    sendCsv("/api/bookings/import/apply")
        .then(data => {
            document.getElementById("import-result").textContent =
                "Создано: " + data.created + ", обновлено: " + data.updated + ", ошибок: " + data.errorCount;
            loadBookings();
        })
        .catch(error => show(error.message));
});
document.getElementById("note-form").addEventListener("submit", async event => {
    event.preventDefault();
    if (!state.selected) return;
    await api("/api/bookings/" + state.selected.id + "/notes", {
        method: "POST",
        body: JSON.stringify({
            text: document.getElementById("note-text").value,
            pinned: document.getElementById("note-pinned").checked
        })
    });
    document.getElementById("note-text").value = "";
    loadNotes(state.selected.id);
});
document.getElementById("passenger-form").addEventListener("submit", async event => {
    event.preventDefault();
    try {
        await api("/api/passengers", {
            method: "POST",
            body: JSON.stringify({
                firstName: document.getElementById("p-first").value,
                lastName: document.getElementById("p-last").value,
                email: document.getElementById("p-email").value,
                phone: document.getElementById("p-phone").value,
                passportNumber: document.getElementById("p-passport").value
            })
        });
        event.target.reset();
        show("Пассажир создан.", true);
        loadPassengers();
    } catch (error) {
        show(error.message);
    }
});
document.getElementById("user-form").addEventListener("submit", async event => {
    event.preventDefault();
    const roles = Array.from(document.querySelectorAll("input[name=role]:checked")).map(node => node.value);
    try {
        const result = await api("/api/admin/users", {
            method: "POST",
            body: JSON.stringify({
                email: document.getElementById("u-email").value,
                name: document.getElementById("u-name").value,
                roles
            })
        });
        show("Пользователь создан. Временный пароль: " + result.temporaryPassword, true);
        loadUsers();
    } catch (error) {
        show(error.message);
    }
});
document.getElementById("audit-search").addEventListener("click", () => loadAudit().catch(error => show(error.message)));

if (state.token) {
    api("/api/auth/me").then(user => {
        state.user = user;
        showApp();
    }).catch(() => sessionStorage.removeItem("access"));
}
