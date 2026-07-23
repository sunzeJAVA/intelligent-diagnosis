using CSharpParser.Services;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddGrpc();
builder.Services.AddGrpcHealthChecks()
    .AddCheck("CSharpParser", () => Microsoft.Extensions.Diagnostics.HealthChecks.HealthCheckResult.Healthy());

builder.Services.AddSingleton<CSharpAnalyzer>();

var app = builder.Build();

app.MapGrpcService<CSharpParseWorkerService>();
app.MapGrpcHealthChecksService();
app.MapGet("/", () => "C# Parse Worker");

app.Run();
