# Adapter Application API Documentation

This document provides comprehensive documentation for all APIs in the Adapter Application, based on Spring Reactor and published via Swagger/OpenAPI.

## Overview

The application exposes REST APIs for importing data, exporting data, managing files, and tracking export runs. APIs are built with Spring WebFlux for reactive programming.

Swagger UI is available at: `http://localhost:9001/swagger-ui.html` (assuming default port 9001)

OpenAPI spec: `http://localhost:9001/v3/api-docs`

## Controllers and Endpoints

### 1. AdapterController

Handles import and export triggers for various data types.

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

#### POST /import/sensordata
Triggers sensor data import.

**Request:**
- Method: POST
- Content-Type: N/A
- Body: None

**Response:**
- Status: 200 OK
- Body: None (Void)

**Description:** Initiates the import process for sensor data.

#### POST /import/vocabulary
Triggers vocabulary data import.

**Request:**
- Method: POST
- Content-Type: N/A
- Body: None

**Response:**
- Status: 200 OK
- Body: None (Void)

**Description:** Initiates the import process for vocabulary data.

#### POST /export/costs
Triggers cost data export.

**Request:**
- Method: POST
- Content-Type: N/A
- Body: None

**Response:**
- Status: 200 OK
- Body: None (Void)

**Description:** Initiates the export process for cost data, tracked via ExportTrackingService.

#### POST /export/vocabulary
Triggers vocabulary data export.

**Request:**
- Method: POST
- Content-Type: N/A
- Body: None

**Response:**
- Status: 200 OK
- Body: None (Void)

**Description:** Initiates the export process for vocabulary data, tracked via ExportTrackingService.

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

### 3. ExportTrackingController

Manages and retrieves information about export runs.

#### GET /exports
Retrieves a paginated list of export runs with optional filtering.

**Request:**
- Method: GET
- Query Parameters:
  - `from` (LocalDateTime, ISO format): Start date filter
  - `to` (LocalDateTime, ISO format): End date filter
  - `type` (String): Exporter type filter
  - `status` (String): Status filter
  - `limit` (int, default 20): Page size
  - `offset` (int, default 0): Page offset

**Response:**
- Status: 200 OK
- Content-Type: application/json
- Body: Page<ExportRunDTO>

**ExportRunDTO Schema:**
```json
{
  "id": "long",
  "exporterType": "string",
  "exportName": "string",
  "status": "string",
  "startedAt": "LocalDateTime",
  "finishedAt": "LocalDateTime",
  "durationMs": "long",
  "exportedFiles": ["string"],
  "uploadSuccess": "boolean",
  "errorMessage": "string",
  "requestParams": "string"
}
```

#### GET /exports/{id}
Retrieves details of a specific export run.

**Request:**
- Method: GET
- Path Parameter: `id` (Long) - Export run ID

**Response:**
- Status: 200 OK / 404 Not Found
- Content-Type: application/json
- Body: ExportRunDTO

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

### 5. InfluxExportController

Handles export of InfluxDB bucket data.

#### GET /export/influxdb/buckets
Retrieves a list of available InfluxDB buckets for export.

**Request:**
- Method: GET
- Content-Type: N/A

**Response:**
- Status: 200 OK / 500 Internal Server Error
- Content-Type: application/json
- Body: InfluxBucketResponse

**InfluxBucketResponse Schema:**
```json
{
  "success": true,
  "buckets": [
    {
      "name": "string",
      "bucketName": "string",
      "description": "string"
    }
  ],
  "timestamp": "Instant"
}
```

#### POST /export/influxdb
Exports data from selected InfluxDB buckets with optional time range filtering.

**Request:**
- Method: POST
- Content-Type: application/json
- Body: InfluxExportRequest

**InfluxExportRequest Schema:**
```json
{
  "bucket": "string",
  "startTime": "string (ISO-8601)",
  "endTime": "string (ISO-8601)"
}
```

**Response:**
- Status: 200 OK / 400 Bad Request / 500 Internal Server Error
- Content-Type: application/json
- Body: InfluxExportResponse

**InfluxExportResponse Schema:**
```json
{
  "success": true,
  "message": "string",
  "exportedFiles": ["string"],
  "timestamp": "Instant"
}
```

**Description:** Exports InfluxDB data asynchronously. Time range uses ISO-8601 format (e.g., 2024-01-15T10:30:00). Defaults to 5 years ago if startTime not provided, current time if endTime not provided.

## Error Handling

- 400 Bad Request: Invalid parameters or malformed requests
- 404 Not Found: Resource not found
- 500 Internal Server Error: Server-side errors

Response bodies for errors typically include error messages in the respective DTOs.

## Authentication and Security

The application uses Spring Security configuration (see SecurityConfig.java). Specific authentication mechanisms depend on the deployment environment.

## Configuration

APIs are configured via application.yaml with environment variables for database connections, file paths, and external service credentials.

### 6. ClimateController

Exposes current climate sensor readings sourced from InfluxDB.

#### GET /climate/readings
Returns the most recent temperature reading for each configured climate sensor.

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
    "measuredAt": "2026-05-16T10:00:00Z"
  }
]
```

**Sample curl:**
```bash
curl -s http://localhost:9001/climate/readings | jq .
```

**Description:** Queries the `sensor_data` InfluxDB bucket for the latest `°C` measurement matching the configured `entity_id` (default `draussen_temperature`). Returns an empty JSON array when no data is available. The entity ID is configurable via the `CLIMATE_OUTDOOR_ENTITY_ID` environment variable.

## Reactive Programming

All APIs are built with Spring WebFlux and Reactor, supporting non-blocking, asynchronous operations suitable for high-throughput scenarios.