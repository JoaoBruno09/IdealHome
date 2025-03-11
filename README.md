# 🏠 IdealHome

IdealHome is a back-end application that finds real estate properties on Idealista tailored to your specific needs. It leverages Notion to record properties you’ve already seen and uses a Telegram bot to notify a designated group whenever a new property is discovered.

## 🔍 Key Features
- <b>Automated Search:</b> Queries Idealista API everyday at 1pm, to find properties matching your criteria.
- <b>Duplicate Prevention:</b> Uses Notion to keep track of viewed properties so you don’t see the same listings twice.
- <b>Real-Time Notifications:</b> Sends instant updates to a Telegram group when a new property is found.
- <b>Customizable Criteria:</b> Easily adjust search parameters to suit your needs.

## 👨‍💻 Technologies
<div style="display: inline_block"><br>
<img align="center" alt="Java" height="40" width="40" src="https://github.com/devicons/devicon/blob/master/icons/java/java-original.svg">
<img align="center" alt="Spring" height="40" width="40" src="https://github.com/devicons/devicon/blob/master/icons/spring/spring-original.svg">
<img align="center" alt="Idealista" height="40" width="40" src="https://avatars.githubusercontent.com/u/12275373?s=400&u=5fefa7963d805bef05f73a765e43f9f7dcfbc310&v=4">
<img align="center" alt="Notion" height="40" width="40" src="https://github.com/devicons/devicon/blob/master/icons/notion/notion-original.svg">
<img align="center" alt="Telegram" height="40" width="40" src="https://upload.wikimedia.org/wikipedia/commons/8/83/Telegram_2019_Logo.svg">
</div>

## 📂 Repository Structure

The repository is organized as follows:

- `idealhome/src/main/resources`: Contains all the configs including: API URLs, API KEYs, and filters according to your needs.
- `idealhome/src/main/java/com/api/idealhome/configs`: Folder imports all the configs, from `idealhome/src/main/resources`, to make the application runnable.
- `idealhome/src/main/java/com/api/idealhome/clients`: Folder contains the Idealista API, Notion API, and Telegram Bot API to make use of them.
- `idealhome/src/main/java/com/api/idealhome/models/dtos`: Folder contains all the DTOs needed to communicate with those APIs.
- `idealhome/src/main/java/com/api/idealhome/services/impl`: Folder contains the application's logic.

## 🚀 Getting Started

To get started with the projects in this repository, follow these steps:

1. Clone the repository to your local machine using the following command:

   ```bash
   git clone https://github.com/JoaoBruno09/IdealHome.git
   ```
2. Change the configurations in `idealhome/src/main/resources`

3. Run the application on your local machine.

Explore each folder to find specific code samples you want to review or work on.

## Prerequisites 📋

Before running the projects, make sure you have the following prerequisites installed on your machine:

- JDK 21
- Java IDE (eg: IntelliJ IDEA)

## 🌟 Additional Resources

[Idealista API 🏘️](https://developers.idealista.com/access-request)  
[Notion API 📖](https://developers.notion.com/docs/getting-started)  
[Telegram Bot API 🤖](https://core.telegram.org/bots/api)  