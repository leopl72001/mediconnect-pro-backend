-- Esquema exclusivo de MediConnect Pro. Para base nueva; NO modifica la versión 1.
CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    email VARCHAR(200) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL
);

CREATE TABLE specialties (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE offices (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    room_number VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE TABLE doctors (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES app_users(id),
    specialty_id UUID NOT NULL REFERENCES specialties(id),
    medical_license VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE patient_profiles (
    user_id UUID PRIMARY KEY REFERENCES app_users(id),
    birth_date DATE,
    phone VARCHAR(30),
    address VARCHAR(500)
);

CREATE TABLE appointments (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES app_users(id),
    doctor_id UUID NOT NULL REFERENCES doctors(id),
    office_id UUID NOT NULL REFERENCES offices(id),
    start_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    status VARCHAR(255) NOT NULL
);
CREATE INDEX idx_appointments_doctor_time ON appointments(doctor_id, start_time, end_time);

CREATE TABLE prescriptions (
    id UUID PRIMARY KEY,
    appointment_id UUID NOT NULL REFERENCES appointments(id),
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE TABLE prescription_items (
    prescription_id UUID NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    medication VARCHAR(255) NOT NULL,
    dosage VARCHAR(255) NOT NULL,
    frequency VARCHAR(255) NOT NULL,
    validation_status VARCHAR(255) NOT NULL,
    PRIMARY KEY (prescription_id, line_number)
);
