default_agent: {{ name }}
agents:
  {{ name }}:
    base:
      name: {{ name }}
      entrypoint: com.example.{{ name }}Agent
      region: {{ region }}
      language: java17
      base_image: eclipse-temurin:17-jre
      dependency_file: pom.xml
      platform: linux/amd64
      container_runtime: docker
    swr_config:
      organization: {{ swr_org }}
      repository: {{ swr_repo }}
      organization_auto_create: true
      repository_auto_create: true
    runtime:
      invoke_config:
        protocol: HTTP
        port: 8080
