# DALI E-commerce Application

This is a Spring Boot application for the DALI e-commerce site.

## Getting Started

### Prerequisites
- Java 21
- Apache Maven
- PostgreSQL

### Local Setup Instructions

1.  **Clone the repository:**
    ```bash
    git clone <your-repo-url>
    cd DALI
    ```

2.  **Set up the PostgreSQL Database:**
    - Make sure PostgreSQL is installed and running.
    - Create a database named `dali_db`.
    ```sql
    CREATE DATABASE dali_db;
    ```

3.  **Configure Environment Variables:**
    The application requires database credentials to be set as environment variables.

    - **On Windows (Command Prompt):**
      ```cmd
      set DB_PASSWORD=your_postgres_password
      ```

4.  **Run the Application:**
    You can run the application using the Maven wrapper:
    ```bash
    ./mvnw spring-boot:run
    ```
    The application will be available at `http://localhost:8080`. The database schema and initial data will be automatically loaded on the first run.