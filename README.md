# 🖤 DarkMind Diary  
**Tu Espacio de Bienestar Emocional Digital e Inteligente**

DarkMind Diary es una aplicación móvil para Android diseñada para ser tu compañero personal en la gestión y comprensión de tus emociones. Ofrece un espacio privado y seguro donde puedes registrar tus pensamientos y sentimientos a través de texto, imágenes y grabaciones de audio. Utilizando el poder del *Machine Learning*, DarkMind te proporciona un análisis de sentimiento automatizado, ayudándote a visualizar y reflexionar sobre tus estados emocionales a lo largo del tiempo.

---

## 🌟 Características Principales

- **📝 Registro Multimodal**: Escribe entradas, adjunta imágenes desde galería o cámara, y graba notas de voz.
- **🤖 Análisis de Sentimiento Inteligente**: Detecta la polaridad emocional (positiva, negativa, neutral) de tus entradas.
- **☁️ Sincronización en la Nube**: Guardado automático y seguro en Firebase (Firestore y Storage).
- **📶 Funcionalidad Offline**: Tus registros se guardan localmente y se sincronizan al volver a tener internet.
- **🖤 Interfaz Intuitiva y Minimalista**: Diseño oscuro, moderno y enfocado en la introspección.
- **🔐 Autenticación Robusta**: Accede con correo/contraseña o Google Sign-In.

---

## 🚀 Tecnologías Utilizadas

- **Lenguaje y Plataforma**: Kotlin, Android SDK (API 26+)
- **Backend**: Firebase Authentication, Firestore, Storage
- **ML Local**: TensorFlow Lite (modelo de análisis de sentimientos)
- **Persistencia Local**: Room (Jetpack)
- **Concurrencia**: Kotlin Coroutines
- **Gestión de Proyecto**: ClickUp (metodología ágil)

---

## 🏗️ Arquitectura del Sistema

DarkMind Diary sigue una arquitectura **Cliente-Servidor**, donde:

- El cliente Android maneja la UI, lógica y almacenamiento local.
- Firebase proporciona autenticación, base de datos NoSQL y almacenamiento de medios.
- El análisis de sentimiento se realiza **localmente** en el dispositivo para proteger la privacidad.

```mermaid
graph TD
    subgraph UI ["Capa de Presentación"]
        A[Activities/Fragments] --> B(ViewModels)
    end

    subgraph Data ["Capa de Datos"]
        B --> C[EntryRepository]
        C --> D[Room Database]
        C --> E[Firebase Firestore]
        C --> F[Firebase Storage]
    end

    subgraph ML ["Módulo de Machine Learning"]
        A --- G[SentimentAnalyzer]
    end

    subgraph ExternalServices ["Servicios Externos"]
        E --- H(Firebase Authentication)
        F --- H
    end

    D -- Persistencia Local --> C
    E -- Persistencia en la Nube --> C
    F -- Almacenamiento Multimedia --> C
    G -- Análisis de Texto --> A
    H -- Autenticación de Usuarios --> A

    style A fill:#ADD8E6,stroke:#333,stroke-width:2px;
    style B fill:#90EE90,stroke:#333,stroke-width:2px;
    style C fill:#FFD700,stroke:#333,stroke-width:2px;
    style D fill:#FFB6C1,stroke:#333,stroke-width:2px;
    style E fill:#FFB6C1,stroke:#333,stroke-width:2px;
    style F fill:#ADD8E6,stroke:#333,stroke-width:2px;
    style G fill:#DA70D6,stroke:#333,stroke-width:2px;
    style H fill:#87CEEB,stroke:#333,stroke-width:2px;
