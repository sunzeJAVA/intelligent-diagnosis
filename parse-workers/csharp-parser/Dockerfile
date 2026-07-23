# Build stage
FROM mcr.microsoft.com/dotnet/sdk:8.0-alpine AS builder

WORKDIR /build

COPY shared-contracts/proto/ shared-contracts/proto/
COPY parse-workers/csharp-parser/ ./

RUN dotnet restore
RUN dotnet publish -c Release -o /app/publish --no-restore

# Runtime stage
FROM mcr.microsoft.com/dotnet/aspnet:8.0-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /app/publish .

USER appuser

EXPOSE 8084 9094

ENTRYPOINT ["dotnet", "CSharpParser.dll"]
