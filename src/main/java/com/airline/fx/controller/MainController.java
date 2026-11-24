package com.airline.fx.controller;

import com.airline.exception.BusinessRuleException;
import com.airline.exception.DatabaseException;
import com.airline.exception.EntityNotFoundException;
import com.airline.fx.ExceptionMessages;
import com.airline.fx.FxDialogs;
import com.airline.model.Booking;
import com.airline.model.BookingStatus;
import com.airline.model.Passenger;
import com.airline.service.BookingService;
import com.airline.service.PassengerService;
import com.airline.util.ExcelExporter;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class MainController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final String ALL_STATUSES = "Все статусы";
    private static final String ALL_CITIES = "Все города";
    private static final String SORT_DATE_ASC = "По дате вылета (сначала ранние)";
    private static final String SORT_DATE_DESC = "По дате вылета (сначала поздние)";
    private static final String SORT_PRICE_ASC = "По цене (сначала дешевле)";
    private static final String SORT_PRICE_DESC = "По цене (сначала дороже)";

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> cityFilter;
    @FXML private ComboBox<String> sortBox;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private TableView<Booking> bookingTable;
    @FXML private TableColumn<Booking, Long> idColumn;
    @FXML private TableColumn<Booking, String> flightColumn;
    @FXML private TableColumn<Booking, String> passengerColumn;
    @FXML private TableColumn<Booking, String> departureColumn;
    @FXML private TableColumn<Booking, String> arrivalColumn;
    @FXML private TableColumn<Booking, LocalDateTime> dateColumn;
    @FXML private TableColumn<Booking, BigDecimal> priceColumn;
    @FXML private TableColumn<Booking, String> statusColumn;
    @FXML private Label detailsLabel;
    @FXML private Label statusLabel;

    @FXML private Button passengerEditButton;
    @FXML private Button passengerDeleteButton;
    @FXML private TableView<Passenger> passengerTable;
    @FXML private TableColumn<Passenger, Long> passengerIdColumn;
    @FXML private TableColumn<Passenger, String> passengerNameColumn;
    @FXML private TableColumn<Passenger, String> passengerEmailColumn;
    @FXML private TableColumn<Passenger, String> passengerPhoneColumn;
    @FXML private TableColumn<Passenger, String> passengerPassportColumn;
    @FXML private Label passengerDetailsLabel;

    private final ObservableList<Booking> bookings = FXCollections.observableArrayList();
    private final ObservableList<Passenger> passengers = FXCollections.observableArrayList();

    private BookingService bookingService;
    private PassengerService passengerService;
    private boolean suppressReload = true;

    @FXML
    private void initialize() {
        bookingTable.setItems(bookings);
        bookingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        bookingTable.setPlaceholder(new Label("Нет бронирований. Измените поиск или добавьте запись."));
        idColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getId()));
        flightColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFlightNumber()));
        passengerColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPassengerFullName()));
        departureColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDepartureCity()));
        arrivalColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getArrivalCity()));
        dateColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getDepartureDate()));
        dateColumn.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? null : date.format(DATE_FORMAT));
            }
        });
        priceColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getPrice()));
        priceColumn.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(BigDecimal price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(price.setScale(2, RoundingMode.HALF_UP) + " руб.");
                }
            }
        });
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().getDisplayName()));
        bookingTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> showBookingDetails(selected));
        editButton.disableProperty().bind(bookingTable.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(bookingTable.getSelectionModel().selectedItemProperty().isNull());

        passengerTable.setItems(passengers);
        passengerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        passengerTable.setPlaceholder(new Label("Пассажиров пока нет."));
        passengerIdColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getId()));
        passengerNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        passengerEmailColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        passengerPhoneColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPhone()));
        passengerPassportColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPassportNumber()));
        passengerTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> showPassengerDetails(selected));
        passengerEditButton.disableProperty().bind(passengerTable.getSelectionModel().selectedItemProperty().isNull());
        passengerDeleteButton.disableProperty().bind(passengerTable.getSelectionModel().selectedItemProperty().isNull());

        statusFilter.getItems().add(ALL_STATUSES);
        for (BookingStatus status : BookingStatus.values()) {
            statusFilter.getItems().add(status.getDisplayName());
        }
        statusFilter.setValue(ALL_STATUSES);
        cityFilter.getItems().add(ALL_CITIES);
        cityFilter.setValue(ALL_CITIES);
        sortBox.getItems().addAll(SORT_DATE_ASC, SORT_DATE_DESC, SORT_PRICE_ASC, SORT_PRICE_DESC);
        sortBox.setValue(SORT_DATE_ASC);
    }

    public void init(BookingService bookingService, PassengerService passengerService) {
        this.bookingService = bookingService;
        this.passengerService = passengerService;
        suppressReload = false;
        reloadBookings();
        reloadPassengers();
    }

    @FXML
    private void onSearch() {
        reloadBookings();
    }

    @FXML
    private void onReset() {
        suppressReload = true;
        searchField.clear();
        statusFilter.setValue(ALL_STATUSES);
        cityFilter.setValue(ALL_CITIES);
        sortBox.setValue(SORT_DATE_ASC);
        suppressReload = false;
        reloadBookings();
    }

    @FXML
    private void onRefresh() {
        reloadBookings();
        statusLabel.setText("Список обновлен из базы.");
    }

    @FXML
    private void onAddBooking() {
        boolean saved = BookingFormController.show(window(), bookingService, passengerService, null);
        if (saved) {
            reloadBookings();
            reloadPassengers();
            statusLabel.setText("Бронирование добавлено.");
        }
    }

    @FXML
    private void onEditBooking() {
        Booking selected = bookingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            FxDialogs.showError(window(), "Выберите бронирование в таблице.");
            return;
        }
        boolean saved = BookingFormController.show(window(), bookingService, passengerService, selected);
        if (saved) {
            reloadBookings();
            reloadPassengers();
            statusLabel.setText("Бронирование сохранено.");
        }
    }

    @FXML
    private void onDeleteBooking() {
        Booking selected = bookingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            FxDialogs.showError(window(), "Выберите бронирование в таблице.");
            return;
        }
        String message = "Удалить бронирование рейса " + selected.getFlightNumber()
                + " (" + selected.getPassengerFullName() + ")?";
        if (!FxDialogs.confirmDelete(window(), message)) {
            return;
        }
        try {
            bookingService.deleteBooking(selected.getId());
            reloadBookings();
            statusLabel.setText("Бронирование удалено.");
        } catch (EntityNotFoundException | DatabaseException e) {
            FxDialogs.showError(window(), ExceptionMessages.toUserText(e));
        }
    }

    @FXML
    private void onStatistics() {
        StatisticsController.show(window(), bookingService);
    }

    @FXML
    private void onExportExcel() {
        exportCurrent("bookings.xlsx", "Excel", "*.xlsx", true);
    }

    @FXML
    private void onExportCsv() {
        exportCurrent("bookings.csv", "CSV", "*.csv", false);
    }

    private void exportCurrent(String fileName, String filterName, String pattern, boolean excel) {
        if (bookings.isEmpty()) {
            FxDialogs.showError(window(), "Нет данных для экспорта. Измените поиск или фильтры.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить бронирования");
        chooser.setInitialFileName(fileName);
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(filterName, pattern));
        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }
        String path = file.getAbsolutePath();
        String extension = excel ? ".xlsx" : ".csv";
        if (!path.toLowerCase().endsWith(extension)) {
            path = path + extension;
        }
        try {
            if (excel) {
                ExcelExporter.exportToExcel(new ArrayList<>(bookings), path);
            } else {
                ExcelExporter.exportToCsv(new ArrayList<>(bookings), path);
            }
            statusLabel.setText("Файл сохранен.");
            FxDialogs.showInfo(window(), "Текущий список сохранен в файл:\n" + path);
        } catch (BusinessRuleException e) {
            FxDialogs.showError(window(), ExceptionMessages.toUserText(e));
        }
    }

    @FXML
    private void onAddPassenger() {
        Passenger created = PassengerFormController.show(window(), passengerService, null);
        if (created != null) {
            reloadPassengers();
            reloadBookings();
            statusLabel.setText("Пассажир добавлен.");
        }
    }

    @FXML
    private void onEditPassenger() {
        Passenger selected = passengerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            FxDialogs.showError(window(), "Выберите пассажира в таблице.");
            return;
        }
        Passenger saved = PassengerFormController.show(window(), passengerService, selected);
        if (saved != null) {
            reloadPassengers();
            reloadBookings();
            statusLabel.setText("Данные пассажира сохранены.");
        }
    }

    @FXML
    private void onDeletePassenger() {
        Passenger selected = passengerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            FxDialogs.showError(window(), "Выберите пассажира в таблице.");
            return;
        }
        int bookingCount = bookingService.getBookingsByPassengerId(selected.getId()).size();
        String message = bookingCount == 0
                ? "Удалить пассажира " + selected.getFullName() + "?"
                : "Удалить пассажира " + selected.getFullName()
                + "? Его бронирования тоже удалятся: " + bookingCount + ".";
        if (!FxDialogs.confirmDelete(window(), message)) {
            return;
        }
        try {
            passengerService.deletePassenger(selected.getId());
            reloadPassengers();
            reloadBookings();
            statusLabel.setText("Пассажир удален.");
        } catch (EntityNotFoundException | DatabaseException e) {
            FxDialogs.showError(window(), ExceptionMessages.toUserText(e));
        }
    }

    @FXML
    private void onRefreshPassengers() {
        reloadPassengers();
    }

    private void reloadBookings() {
        if (suppressReload || bookingService == null) {
            return;
        }
        try {
            fillCities();
            long selectedId = selectedBookingId();
            List<Booking> view = bookingService.prepareTable(
                    searchField.getText(),
                    selectedStatus(),
                    selectedCity(),
                    sortField(),
                    sortAscending()
            );
            bookings.setAll(view);
            restoreBookingSelection(selectedId);
            if (bookingTable.getSelectionModel().getSelectedItem() == null) {
                showBookingDetails(null);
            }
        } catch (RuntimeException e) {
            FxDialogs.showError(window(), ExceptionMessages.toUserText(e));
        }
    }

    private void fillCities() {
        String selected = cityFilter.getValue();
        TreeSet<String> cities = new TreeSet<>();
        for (Booking booking : bookingService.getAllBookings()) {
            cities.add(booking.getDepartureCity());
            cities.add(booking.getArrivalCity());
        }
        suppressReload = true;
        cityFilter.getItems().setAll(ALL_CITIES);
        cityFilter.getItems().addAll(cities);
        if (selected != null && cityFilter.getItems().contains(selected)) {
            cityFilter.setValue(selected);
        } else {
            cityFilter.setValue(ALL_CITIES);
        }
        suppressReload = false;
    }

    private void reloadPassengers() {
        if (passengerService == null) {
            return;
        }
        try {
            long selectedId = -1;
            Passenger selected = passengerTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selectedId = selected.getId();
            }
            passengers.setAll(passengerService.getAllPassengers());
            if (selectedId > 0) {
                for (Passenger passenger : passengers) {
                    if (passenger.getId() == selectedId) {
                        passengerTable.getSelectionModel().select(passenger);
                        break;
                    }
                }
            }
            if (passengerTable.getSelectionModel().getSelectedItem() == null) {
                showPassengerDetails(null);
            }
        } catch (RuntimeException e) {
            FxDialogs.showError(window(), ExceptionMessages.toUserText(e));
        }
    }

    private void showBookingDetails(Booking booking) {
        if (booking == null) {
            detailsLabel.setText("Выберите строку в таблице, чтобы увидеть маршрут и данные пассажира.");
            return;
        }
        String passengerText;
        try {
            Passenger passenger = passengerService.getPassengerById(booking.getPassengerId());
            passengerText = passenger.getFullName()
                    + ", " + passenger.getEmail()
                    + ", " + passenger.getPhone()
                    + ", паспорт " + passenger.getPassportNumber();
        } catch (RuntimeException e) {
            passengerText = booking.getPassengerFullName();
        }
        detailsLabel.setText("Рейс " + booking.getFlightNumber()
                + ", " + booking.getDepartureCity() + " — " + booking.getArrivalCity()
                + ", вылет " + booking.getDepartureDate().format(DATE_FORMAT)
                + ", " + booking.getPrice().setScale(2, RoundingMode.HALF_UP) + " руб., статус "
                + booking.getStatus().getDisplayName() + ".\nПассажир: " + passengerText);
    }

    private void showPassengerDetails(Passenger passenger) {
        if (passenger == null || bookingService == null) {
            passengerDetailsLabel.setText("Выберите пассажира, чтобы увидеть его бронирования.");
            return;
        }
        try {
            List<Booking> related = bookingService.getBookingsByPassengerId(passenger.getId());
            if (related.isEmpty()) {
                passengerDetailsLabel.setText("У пассажира " + passenger.getFullName() + " нет бронирований.");
                return;
            }
            StringBuilder flights = new StringBuilder();
            for (Booking booking : related) {
                if (flights.length() > 0) {
                    flights.append(", ");
                }
                flights.append(booking.getFlightNumber());
            }
            passengerDetailsLabel.setText("У пассажира " + passenger.getFullName()
                    + " бронирований: " + related.size() + ". Рейсы: " + flights);
        } catch (RuntimeException e) {
            passengerDetailsLabel.setText(ExceptionMessages.toUserText(e));
        }
    }

    private BookingStatus selectedStatus() {
        String value = statusFilter.getValue();
        if (value == null || ALL_STATUSES.equals(value)) {
            return null;
        }
        for (BookingStatus status : BookingStatus.values()) {
            if (status.getDisplayName().equals(value)) {
                return status;
            }
        }
        return null;
    }

    private String selectedCity() {
        String value = cityFilter.getValue();
        if (value == null || ALL_CITIES.equals(value)) {
            return null;
        }
        return value;
    }

    private String sortField() {
        String value = sortBox.getValue();
        if (value != null && value.startsWith("По цене")) {
            return "price";
        }
        return "date";
    }

    private boolean sortAscending() {
        String value = sortBox.getValue();
        return value == null || value.contains("ранние") || value.contains("дешевле");
    }

    private long selectedBookingId() {
        Booking selected = bookingTable.getSelectionModel().getSelectedItem();
        return selected == null ? -1 : selected.getId();
    }

    private void restoreBookingSelection(long selectedId) {
        if (selectedId <= 0) {
            return;
        }
        for (Booking booking : bookings) {
            if (booking.getId() == selectedId) {
                bookingTable.getSelectionModel().select(booking);
                bookingTable.scrollTo(booking);
                return;
            }
        }
    }

    private Window window() {
        if (bookingTable.getScene() == null) {
            return null;
        }
        return bookingTable.getScene().getWindow();
    }
}
