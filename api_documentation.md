# Adapter Application API Documentation

This document provides comprehensive documentation for all APIs in the Adapter Application, based on Spring Reactor and published via Swagger/OpenAPI.

## Overview

The application exposes REST APIs for importing data, managing files, and tracking backup runs. APIs are built with Spring WebFlux for reactive programming.

Swagger UI is available at: `http://localhost:9001/swagger-ui.html` (assuming default port 9001)

OpenAPI spec: `http://localhost:9001/v3/api-docs`

## Controllers and Endpoints

### 1. AdapterController

Handles import triggers for cost data.

#### POST /import/costs
Triggers cost data import.

**Request:**
- Method: POST
- Content-Type: N/A
- Body: None

**Response:**
- Status: 200 OK
- Body: None (Void)

**Description:** Initiates the import process for cost data from configured sources.

### 2. CamtController

Handles CAMT (Cash Management) file processing for banking transactions.

#### POST /camt-entries
Parses and extracts booking entries from uploaded CAMT zip files.

**Request:**
- Method: POST
- Content-Type: multipart/form-data
- Body: Multipart file with key "file" (zip file containing CAMT XML files)

**Response:**
- Status: 200 OK
- Content-Type: application/json
- Body: `BookingsDTO`

**BookingsDTO Schema:**
```json
{
  "bookingsPerMonth": [
    {
      "year": 2023,
      "month": 12,
      "usualBookings": [...],
      "dailyCosts": [...],
      "incomes": [...]
    }
  ]
}
```

**BookingEntryDTO Schema:**
```json
{
  "creditDebitCode": "CRDT|DBIT",
  "entryInfo": "string",
  "amount": "BigDecimal",
  "bookingDate": "LocalDate",
  "firstOfMonth": "LocalDate",
  "debitName": "string",
  "debitIban": "string",
  "creditName": "string",
  "creditIban": "string",
  "additionalInfo": "string"
}
```

**Description:** Uploads a zip file containing CAMT.052.001.08 XML files, parses them, and returns categorized booking entries grouped by month.

### 3. BackupRunController

Manages and retrieves information about backup upload runs.

#### GET /backups
Retrieves a paginated list of backup runs with optional filtering.

**Request:**
- Method: GET
- Query Parameters:
  - `from` (LocalDateTime, ISO format): Start date filter
  - `to` (LocalDateTime, ISO format): End date filter
  - `status` (String): Status filter (SUCCESS, FAILED, IN_PROGRESS)
  - `limit` (int, default 20): Page size
  - `offset` (int, default 0): Page offset

**Response:**
- Status: 200 OK
- Content-Type: application/json
- Body: Page<BackupRunDTO>

**BackupRunDTO Schema:**
```json
{
  "id": "long",
  "fileName": "string",
  "status": "string",
  "startedAt": "LocalDateTime",
  "finishedAt": "LocalDateTime",
  "durationMs": "long",
  "uploadSuccess": "boolean",
  "errorMessage": "string"
}
```

#### GET /backups/{id}
Retrieves details of a specific backup run.

**Request:**
- Method: GET
- Path Parameter: `id` (Long) - Backup run ID

**Response:**
- Status: 200 OK / 404 Not Found
- Content-Type: application/json
- Body: BackupRunDTO

### 4. FileListController

Manages file operations in Google Drive.

#### GET /files/list
Lists all files and folders in the configured Google Drive parent folder.

**Request:**
- Method: GET
- Content-Type: N/A

**Response:**
- Status: 200 OK / 500 Internal Server Error
- Content-Type: application/json
- Body: FileListResponse

**FileListResponse Schema:**
```json
{
  "success": true,
  "message": "string",
  "files": [
    {
      "id": "string",
      "name": "string",
      "size": "long",
      "modifiedTime": "Instant",
      "webViewLink": "string"
    }
  ],
  "timestamp": "Instant"
}
```

#### DELETE /files/{fileId}
Deletes a specific file from Google Drive.

**Request:**
- Method: DELETE
- Path Parameter: `fileId` (String) - Google Drive file ID

**Response:**
- Status: 200 OK / 400 Bad Request / 404 Not Found / 500 Internal Server Error
- Content-Type: application/json
- Body: FileDeleteResponse

**FileDeleteResponse Schema:**
```json
{
  "success": true,
  "message": "string",
  "fileId": "string",
  "timestamp": "Instant"
}
```

## Error Handling

- 400 Bad Request: Invalid parameters or malformed requests
- 404 Not Found: Resource not found
- 500 Internal Server Error: Server-side errors

Response bodies for errors typically include error messages in the respective DTOs.

## Authentication and Security

The application uses Spring Security configuration (see SecurityConfig.java). Specific authentication mechanisms depend on the deployment environment.

## Configuration

APIs are configured via application.yaml with environment variables for database connections, file paths, and external service credentials.

### 5. ClimateController

Exposes current climate sensor readings sourced from InfluxDB.

#### GET /climate/readings
Returns the most recent temperature reading for each configured climate sensor, optionally enriched with the matching humidity reading.

**Request:**
- Method: GET
- Content-Type: N/A
- Body: None

**Response:**
- Status: 200 OK
- Content-Type: application/json
- Body: `TemperatureReading[]`

**TemperatureReading Schema:**
```json
[
  {
    "sensorId": "draussen_temperature",
    "label": "Draußen",
    "location": "outdoor",
    "temperatureC": 21.5,
    "humidityPct": 55.0,
    "measuredAt": "2026-05-16T10:00:00Z"
  }
]
```

`humidityPct` is optional and is omitted from the JSON payload when the humidity sensor has no recent record.

**Sample curl:**
```bash
curl -s http://localhost:9001/climate/readings | jq .
```

**Description:** Queries the `sensor_data` InfluxDB bucket for the latest `°C` measurement matching the configured temperature `entity_id` (default `draussen_temperature`) and the latest `%` measurement matching the humidity `entity_id` (default `draussen_humidity`). Temperature and humidity are queried in parallel and merged into a single reading; a missing humidity record does not block the response. Returns an empty JSON array when no temperature data is available. The entity IDs are configurable via the `CLIMATE_OUTDOOR_ENTITY_ID` and `CLIMATE_OUTDOOR_HUMIDITY_ENTITY_ID` environment variables.

## Reactive Programming

All APIs are built with Spring WebFlux and Reactor, supporting non-blocking, asynchronous operations suitable for high-throughput scenarios.