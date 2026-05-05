Param(
    [string]$Namespace = "smart-freelance",
    [switch]$SkipMaven
)

$ErrorActionPreference = "Stop"

python scripts/render-k8s-secrets.py --repo-root . --mdp-file mdp.local --output k8s/02-secrets.generated.yaml --namespace $Namespace

kubectl apply --dry-run=client --validate=false -f k8s/00-namespace.yaml | Out-Null
kubectl apply --dry-run=client --validate=false -f k8s/01-configmap.yaml | Out-Null
kubectl apply --dry-run=client --validate=false -f k8s/02-secrets.generated.yaml | Out-Null
kubectl apply --dry-run=client --validate=false -f k8s/03-mysql.yaml | Out-Null
kubectl apply --dry-run=client --validate=false -f k8s/04-keycloak.yaml | Out-Null
kubectl apply --dry-run=client --validate=false -f k8s/05-eureka.yaml | Out-Null
kubectl apply --dry-run=client --validate=false -f k8s/06-config-server.yaml | Out-Null
kubectl apply --dry-run=client --validate=false -f k8s/07-api-gateway.yaml | Out-Null
kubectl apply --dry-run=client --validate=false -f k8s/12-ollama.yaml | Out-Null
kubectl apply --dry-run=client --validate=false -f k8s/08-microservices.yaml | Out-Null
kubectl apply --dry-run=client --validate=false -f k8s/09-frontend.yaml | Out-Null
kubectl apply --dry-run=client --validate=false -f k8s/10-ingress.yaml | Out-Null

if (-not $SkipMaven) {
    if (Test-Path ".\backEnd\Microservices\Notification\mvnw.cmd") {
        & .\backEnd\Microservices\Notification\mvnw.cmd -B -f backEnd/Microservices/Notification/pom.xml -DskipTests compile
        & .\backEnd\Microservices\planning\mvnw.cmd -B -f backEnd/Microservices/planning/pom.xml -DskipTests compile
        & .\backEnd\Microservices\AImodel\mvnw.cmd -B -f backEnd/Microservices/AImodel/pom.xml -DskipTests compile
    } else {
        mvn -B -f backEnd/Microservices/Notification/pom.xml -DskipTests compile
        mvn -B -f backEnd/Microservices/planning/pom.xml -DskipTests compile
        mvn -B -f backEnd/Microservices/AImodel/pom.xml -DskipTests compile
    }
}

Write-Host "CI/CD verification completed (vendor and aimodel-node intentionally excluded)."
