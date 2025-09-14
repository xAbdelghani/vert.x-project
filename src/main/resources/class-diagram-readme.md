# Class Diagram for Policy Management Application

This directory contains a PlantUML file (`class-diagram.puml`) that describes the class structure of the Policy Management Application.

## Overview

The class diagram shows:
- All model classes with their attributes
- Repository interfaces and implementations
- Service classes
- Relationships between classes (associations, inheritance, etc.)

## How to Generate the Diagram

To generate a visual representation of the class diagram, you can use one of the following methods:

### Method 1: Online PlantUML Editor

1. Go to [PlantUML Online Editor](https://www.plantuml.com/plantuml/uml/)
2. Copy the contents of `class-diagram.puml` and paste it into the editor
3. The diagram will be generated automatically

### Method 2: Using PlantUML Extension in VS Code

1. Install the "PlantUML" extension in VS Code
2. Open the `class-diagram.puml` file
3. Right-click in the editor and select "Preview Current Diagram"

### Method 3: Using PlantUML JAR

1. Download the PlantUML JAR from [PlantUML website](https://plantuml.com/download)
2. Run the following command:
   ```
   java -jar plantuml.jar class-diagram.puml
   ```
3. This will generate a PNG file with the diagram

## Diagram Description

The class diagram shows the following components:

### Model Classes
- `Compagnie`: Represents an insurance company
- `Vehicule`: Represents a vehicle
- `ModeleVehicule`: Represents a vehicle model
- `Attestation`: Represents an insurance certificate
- `TypeAttestation`: Represents a type of insurance certificate
- `Transaction`: Represents a financial transaction
- And many more...

### Repository Layer
- `CompagnieRepository`: Interface for Compagnie data access
- `CompagnieRepositoryImpl`: Implementation of CompagnieRepository

### Service Layer
- `CompagnieRegistrationService`: Handles registration and account management for companies

### Relationships
The diagram shows various relationships between classes, such as:
- One-to-many relationships (e.g., one Compagnie has many Vehicules)
- Many-to-one relationships (e.g., many Vehicules belong to one ModeleVehicule)
- Many-to-many relationships (e.g., Compagnie and Pointvente through RelationPointventeCompagnie)
