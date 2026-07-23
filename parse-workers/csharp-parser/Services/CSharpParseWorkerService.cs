using Grpc.Core;
using IntelligentDiagnosis.Parse;

namespace CSharpParser.Services;

public class CSharpParseWorkerService : ParseWorker.ParseWorkerBase
{
    private readonly CSharpAnalyzer _analyzer;
    private readonly ILogger<CSharpParseWorkerService> _logger;

    public CSharpParseWorkerService(CSharpAnalyzer analyzer, ILogger<CSharpParseWorkerService> logger)
    {
        _analyzer = analyzer;
        _logger = logger;
    }

    public override Task<ParseResponse> Parse(ParseRequest request, ServerCallContext context)
    {
        var start = DateTimeOffset.UtcNow;
        _logger.LogInformation("Parsing C# files for repository: {Repository}, commit: {CommitHash}",
            request.Repository, request.CommitHash);

        try
        {
            var elements = _analyzer.Analyze(request.RepoPath, request.ChangedFiles.ToList());

            var response = new ParseResponse
            {
                DurationMs = (long)(DateTimeOffset.UtcNow - start).TotalMilliseconds
            };
            response.Elements.AddRange(elements);

            return Task.FromResult(response);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to parse C# files");

            var response = new ParseResponse
            {
                DurationMs = (long)(DateTimeOffset.UtcNow - start).TotalMilliseconds
            };
            response.Errors.Add(new ParseError
            {
                Message = ex.Message,
                Severity = ErrorSeverity.Fatal
            });

            return Task.FromResult(response);
        }
    }

    public override Task<HealthResponse> HealthCheck(HealthRequest request, ServerCallContext context)
    {
        return Task.FromResult(new HealthResponse
        {
            Healthy = true,
            Version = "0.1.0"
        });
    }
}
