using IntelligentDiagnosis.Parse;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using Microsoft.CodeAnalysis.CSharp.Syntax;

namespace CSharpParser.Services;

public class CSharpAnalyzer
{
    public List<CodeElement> Analyze(string repoPath, List<string> files)
    {
        var elements = new List<CodeElement>();

        foreach (var file in files)
        {
            if (!file.EndsWith(".cs", StringComparison.OrdinalIgnoreCase))
            {
                continue;
            }

            var filePath = Path.Combine(repoPath, file);
            if (!File.Exists(filePath))
            {
                continue;
            }

            var code = File.ReadAllText(filePath);
            var tree = CSharpSyntaxTree.ParseText(code);
            var root = tree.GetCompilationUnitRoot();

            ExtractElements(root, file, elements);
        }

        return elements;
    }

    private void ExtractElements(CompilationUnitSyntax root, string filePath, List<CodeElement> elements)
    {
        var types = root.DescendantNodes().OfType<TypeDeclarationSyntax>();

        foreach (var type in types)
        {
            var classElement = new CodeElement
            {
                Id = $"{filePath}#{type.Identifier.Text}",
                Kind = type is InterfaceDeclarationSyntax ? ElementKind.Interface : ElementKind.Class,
                Name = type.Identifier.Text,
                QualifiedName = type.Identifier.Text,
                FilePath = filePath,
                StartLine = type.GetLocation().GetLineSpan().StartLinePosition.Line + 1,
                EndLine = type.GetLocation().GetLineSpan().EndLinePosition.Line + 1
            };

            elements.Add(classElement);

            var methods = type.DescendantNodes().OfType<MethodDeclarationSyntax>();
            foreach (var method in methods)
            {
                var methodElement = new CodeElement
                {
                    Id = $"{classElement.Id}.{method.Identifier.Text}",
                    Kind = ElementKind.Method,
                    Name = method.Identifier.Text,
                    QualifiedName = $"{classElement.QualifiedName}.{method.Identifier.Text}",
                    FilePath = filePath,
                    StartLine = method.GetLocation().GetLineSpan().StartLinePosition.Line + 1,
                    EndLine = method.GetLocation().GetLineSpan().EndLinePosition.Line + 1
                };
                methodElement.Relations.Add(new Relation
                {
                    Kind = RelationKind.Contains,
                    TargetId = classElement.Id
                });

                elements.Add(methodElement);
            }
        }
    }
}
