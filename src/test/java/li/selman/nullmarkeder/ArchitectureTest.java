package li.selman.nullmarkeder;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;

/**
 * Dogfoods this library on itself: every package in {@code li.selman.nullmarkeder} must contain a
 * {@code package-info.java} annotated with {@code @NullMarked}.
 */
@AnalyzeClasses(packages = "li.selman.nullmarkeder", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String ROOT_PACKAGE = "li.selman.nullmarkeder";

    @ArchTest
    void packagesShouldBeAnnotated(JavaClasses classes) throws IOException {
        var rootPackage = classes.getPackage(ROOT_PACKAGE);
        // getSubpackagesInTree() only returns descendants - it excludes rootPackage itself, so it must
        // be checked separately or a package with no subpackages (like this one) would never be checked.
        List<String> violations = Stream.concat(Stream.of(rootPackage), rootPackage.getSubpackagesInTree().stream())
                .filter(pkg -> !pkg.isAnnotatedWith(NullMarked.class))
                .map(pkg -> pkg.getDescription() + " is not annotated with @" + NullMarked.class.getSimpleName())
                .toList();

        if (!violations.isEmpty()) {
            PackageInfoGenerator.main(ROOT_PACKAGE);
        }

        assertThat(violations)
                .as("Not all packages contain a package-info.java file with the required nullability annotations. "
                        + "Ran PackageInfoGenerator to fix - re-run the build.")
                .isEmpty();
    }
}
